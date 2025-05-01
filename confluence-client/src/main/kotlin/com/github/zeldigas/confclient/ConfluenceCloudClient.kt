package com.github.zeldigas.confclient

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.zeldigas.confclient.model.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.ContentType
import io.ktor.serialization.*
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class ConfluenceCloudClient(
    override val confluenceBaseUrl: Url,
    private val apiBase: String,
    private val httpClient: HttpClient,
    private val fallbackClient: ConfluenceClient
) : ConfluenceClient by fallbackClient {

    private val spacesCache: ConcurrentMap<Int, Space> = ConcurrentHashMap()

    companion object {
        private const val PAGE_SIZE = 100
        private val logger = KotlinLogging.logger {}
    }

    override val confluenceApiBaseUrl: Url
        get() = Url(apiBase)

    override suspend fun describeSpace(key: String, includeHome: Boolean): Space {
        val spaces = httpClient.get("$apiBase/spaces") {
            parameter("keys", key)
        }.readApiResponse<SpaceSearchResult>()
        if (spaces.results.isEmpty()) throw SpaceNotFoundException(key)

        val space = spaces.results.first()
        spacesCache[space.id] = space
        return if (includeHome && space.homepageId != null) {
            space.copy(homepage = this.getPageById(space.homepageId))
        } else {
            space
        }
    }

    private suspend fun getSpaceById(id: Int): Space = httpClient.get("$apiBase/spaces/$id").readApiResponse<Space>()

    override suspend fun getPage(
        space: String,
        title: String,
        expansions: Set<PageLoadOptions>
    ): ConfluencePage {
        val results = findPagesWithLoadOptions(space, title, expansions)

        return extractSinglePage(results)
    }

    suspend fun findPagesWithLoadOptions(
        space: String,
        title: String,
        loadOptions: Set<PageLoadOptions>
    ): List<ConfluencePage> {
        val spaceInfo = resolveSpace(space)
        val pages = httpClient.get("$apiBase/spaces/${spaceInfo.id}/pages") {
            parameter("title", title)
            parameter("limit", PAGE_SIZE)
        }.readApiResponse<ConfCloudPageSearchResult>().results.map { page ->
            toConfluencePage(page, spaceInfo)
        }
        if (loadOptions.isNotEmpty()) {
            throw NotImplementedError("Page load options are not supported yet")
        }
        return pages
    }

    override suspend fun getPageById(
        id: String,
        loadOptions: Set<PageLoadOptions>
    ): ConfluencePage {
        var page = getPageById(
            id,
            includeBody = PageLoadOptions.Content in loadOptions,
            includeLabels = PageLoadOptions.Metadata in loadOptions,
            includeProperties = PageLoadOptions.Metadata in loadOptions,
            includeSpace = PageLoadOptions.Space in loadOptions
        )
        if (PageLoadOptions.Attachments in loadOptions) {
           page = page.copy(children = PageChildren(
               getPageAttachments(page.id)
           ))
        }
        return page
    }

    private suspend fun getPageById(id: String,
                                    includeBody: Boolean = false,
                                    includeLabels: Boolean = false,
                                    includeProperties: Boolean = false,
                                    includeSpace: Boolean = false): ConfluencePage {
        val page = httpClient.get("$apiBase/pages/$id"){
            if (includeBody) {
                parameter("body-format", "storage")
            }
            if (includeLabels) {
                parameter("include-labels", "true")
            }
            if (includeProperties) {
                parameter("include-properties", "true")
            }
        }.readApiResponse<ConfCloudPage>()
        val space: Space? = if (includeSpace) {
            spacesCache.getOrPut(page.spaceId) { this.getSpaceById(page.spaceId) }
        } else null
        return toConfluencePage(page, space)
    }

    private fun toConfluencePage(
        page: ConfCloudPage,
        space: Space?
    ): ConfluencePage = ConfluencePage(
        id = page.id,
        type = com.github.zeldigas.confclient.model.ContentType.page,
        status = page.status,
        title = page.title,
        metadata = page.pageMetadata,
        body = page.body,
        version = page.version?.let {
            PageVersionInfo(
                number = it.number,
                minorEdit = it.minorEdit,
                createdAt = it.createdAt
            )
        },
        children = null,
        ancestors = null,
        space = space,
        links = page.links
    )

    private suspend fun getPageAttachments(pageId: String): PageAttachments {
        val attachments = httpClient.get("$apiBase/pages/$pageId/attachments").readApiResponse<CloudPageAttachments>()
        return PageAttachments(
            results = attachments.results.map { Attachment(it.id, it.title, links = it.links) },
            links = attachments.links
        )
    }

    private suspend fun resolveSpace(key: String): Space {
        val space = spacesCache.values.firstOrNull { it.key == key }
        return space ?: this.describeSpace(key, includeHome = false)
    }


    private suspend inline fun <reified T> HttpResponse.readApiResponse(expectSuccess: Boolean = false): T {
        if (expectSuccess && !status.isSuccess()) {
            parseCloudErrorAndThrow()
        }
        val contentType = contentType()
        if (contentType != null && ContentType.Application.Json.match(contentType)) {
            try {
                return body<T>()
            } catch (e: JsonConvertException) {
                parseCloudErrorAndThrow()
            }
        } else {
            throw UnknownConfluenceErrorException(status.value, bodyAsText())
        }
    }

    private suspend fun HttpResponse.parseCloudErrorAndThrow(): Nothing {
        val content = body<ErrorResponse>()
        val firstError = content.errors.first()
        val msg = "${firstError.code}: ${firstError.title}"
        throw ConfluenceApiErrorException(status.value, msg, mapOf("detail" to firstError.detail))
    }


//    {
//  "errors": [
//    {
//      "status": 400,
//      "code": "INVALID_REQUEST_PARAMETER",
//      "title": "Provided value {Docs} for 'id' is not the correct type. Expected type is long.",
//      "detail": null
//    }
//  ]
//}
}

private data class ErrorResponse(val errors: List<ConfluenceError>)

private data class ConfluenceError(val status: Int, val code: String, val title: String, val detail: String?)

private data class ConfCloudPage(
    val id: String,
    val status: String,
    val title: String,
    val version: CloudPageVersion? = null,
    val properties: AttributesCollection<CloudPageProperty>? = null,
    val labels: AttributesCollection<CloudPageLabel>? = null,
    val body: PageBody? = null,
    @JsonProperty("_links")
    val links: Map<String, String> = emptyMap(),
    val spaceId: Int
) {
    val pageMetadata: PageMetadata?
        get() {
            if (properties == null && labels == null) return null
            return PageMetadata(
                labels = labels?.let {
                    PageLabels(
                        results = it.results.map { l -> Label(l.prefix, l.name, l.id) },
                        it.results.size
                    )
                },
                properties = properties?.results?.let { results ->
                    results.associate {
                        it.key to PageProperty(
                            it.id, it.key, it.value, PropertyVersion(
                                it.version.number
                            )
                        )
                    }
                } ?: emptyMap()
            )
        }
}

private data class AttributesCollection<T>(
    val results: List<T>, val meta: OptionalFieldMeta?, @JsonProperty("_links") val links: Map<String, String>
)

private data class CloudPageVersion(
    val number: Int,
    val message: String,
    val minorEdit: Boolean,
    val createdAt: ZonedDateTime,
)

private data class CloudPageLabel(
    val prefix: String, val name: String, val id: String
)

private data class CloudPageProperty(
    val id: String, val key: String, val value: Any?, val version: CloudVersion
)

private data class OptionalFieldMeta(val hasMore: Boolean, val cursor: String?)

private data class CloudVersion(
    val createdAt: ZonedDateTime,
    val message: String,
    val number: Int,
    val minorEdit: Boolean,
    val authorId: String
)

private data class CloudPageAttachments(
    val results: List<Attachment>, val meta: OptionalFieldMeta?, @JsonProperty("_links") val links: Map<String, String>
)

private data class ConfCloudPageSearchResult (
    val results: List<ConfCloudPage>, @JsonProperty("_links") val links: Map<String, String>
)