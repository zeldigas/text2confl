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
import io.ktor.util.toMap
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class ConfluenceCloudClient(
    override val confluenceBaseUrl: Url,
    private val apiBase: String,
    private val httpClient: HttpClient,
    private val fallbackClient: ConfluenceClient,
    private val collectionsConcurrency: Int = 5,
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
        loadOptions: Set<PageLoadOptions>
    ): ConfluencePage {
        val results = findPagesWithLoadOptions(space, title, loadOptions)

        return extractSinglePage(results)
    }

    override suspend fun getPageOrNull(
        space: String,
        title: String,
        loadOptions: Set<PageLoadOptions>
    ): ConfluencePage? {
        val results = findPagesWithLoadOptions(space, title, loadOptions)

        return if (results.isEmpty()) null else extractSinglePage(results)
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
            if (SimplePageLoadOptions.Content in loadOptions) {
                parameter("body-format", "storage")
            }
        }.readApiResponse<ConfCloudPageSearchResult>().results.map { page ->
            toConfluencePage(page, spaceInfo)
        }
        val leftover = loadOptions - setOf(
            SimplePageLoadOptions.Content, SimplePageLoadOptions.ParentId,
            SimplePageLoadOptions.Space
        )
        return if (leftover.isNotEmpty()) {
            coroutineScope {
                pages.map { page ->
                    async {
                        val extra = getPageById(page.id, leftover)
                        page.copy(
                            labels = extra.labels,
                            attachments = extra.attachments,
                            properties = extra.properties,
                        )
                    }
                }.awaitAll()
            }
        } else {
            pages
        }
    }

    override suspend fun getPageById(
        id: String,
        loadOptions: Set<PageLoadOptions>
    ): ConfluencePage {
        var page = getPageById(
            id,
            includeBody = SimplePageLoadOptions.Content in loadOptions,
            includeLabels = SimplePageLoadOptions.Labels in loadOptions,
            includeProperties = loadOptions.any { it is PagePropertyLoad },
            includeSpace = SimplePageLoadOptions.Space in loadOptions
        )
        if (SimplePageLoadOptions.Attachments in loadOptions) {
            page = page.copy(
                attachments = getPageAttachments(page.id)
            )
        }
        return page
    }

    override suspend fun createPageProperty(
        pageId: String,
        name: String,
        value: PagePropertyInput
    ) {
        return httpClient.post("$apiBase/pages/$pageId/properties") {
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "key" to name,
                    "value" to value.value
                )
            )
        }.readApiResponse(expectSuccess = true)
    }

    override suspend fun updatePageProperty(
        pageId: String,
        property: PageProperty,
        value: PagePropertyInput
    ) {
        return httpClient.put("$apiBase/pages/$pageId/properties/${property.id}") {
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "key" to property.key,
                    "value" to value.value,
                    "version" to value.version
                )
            )
        }.readApiResponse(expectSuccess = true)
    }

    override suspend fun findChildPages(
        pageId: String,
        loadOptions: Set<PageLoadOptions>?
    ): List<ConfluencePage> {
        val collectionFetcher = PagedFetcher(
            confluenceBaseUrl,
            rootApiLinks = true,
            { httpClient.get(it).readApiResponse<AttributesCollection<PageChildItem>>() }
        )
        val search = httpClient.get("$apiBase/pages/$pageId/direct-children") {
            parameter("limit", PAGE_SIZE)
        }.readApiResponse<AttributesCollection<PageChildItem>>()
        val childPages = collectionFetcher.fetchAll(search) {
            PagedFetcher.Page(it.results, it.links["next"])
        }.asSequence()
            .filter { "page".equals(it.type, ignoreCase = true) }
            .filter { "current".equals(it.status, ignoreCase = true)  }

        return childPages.chunked(collectionsConcurrency).toList()
            .flatMap { chunk ->
                coroutineScope {
                    chunk.map { async { getPageById(it.id, loadOptions ?: emptySet()) } }
                        .awaitAll()
                }
            }
    }

    override suspend fun createPage(
        value: PageContentInput,
        updateParameters: PageUpdateOptions
    ): ConfluencePage {
        val space = resolveSpace(value.space!!)
        val cloudPage = httpClient.post("$apiBase/pages") {
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "status" to "current",
                    "title" to value.title,
                    "parentId" to value.parentPage,
                    "spaceId" to space.id.toString(),
                    "body" to toPageBody(value)
                )
            )
        }.readApiResponse<ConfCloudPage>(expectSuccess = true)
        return toConfluencePage(cloudPage, space)
    }

    override suspend fun updatePage(
        pageId: String,
        value: PageContentInput,
        updateParameters: PageUpdateOptions
    ): ConfluencePage {
        return performPageUpdate(pageId, buildMap {
            put("id", pageId)
            put("status", "current")
            put("title", value.title)
            put("body", toPageBody(value))
            put("version", versionNode(value.version, updateParameters))
            value.parentPage?.let { put("parentId", it) }
            value.space?.let { put("spaceId", resolveSpace(it).id.toString()) }
        })
    }

    private fun toPageBody(value: PageContentInput): Map<String, String> = mapOf(
        "representation" to "storage",
        "value" to value.content
    )

    override suspend fun renamePage(
        serverPage: ConfluencePage,
        newTitle: String,
        updateParameters: PageUpdateOptions
    ): ConfluencePage {
        val page = httpClient.put("$apiBase/pages/${serverPage.id}/title") {
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "status" to "current",
                    "title" to newTitle
                )
            )
        }.readApiResponse<ConfCloudPage>(expectSuccess = true)
        return toConfluencePage(page, null)
    }


    override suspend fun deletePage(pageId: String) {
        delete("$apiBase/pages/$pageId")
    }

    override suspend fun deleteAttachment(attachmentId: String) {
        delete("$apiBase/attachments/$attachmentId")
    }

    private suspend fun getPageById(
        id: String,
        includeBody: Boolean = false,
        includeLabels: Boolean = false,
        includeProperties: Boolean = false,
        includeSpace: Boolean = false
    ): ConfluencePage {
        val page = httpClient.get("$apiBase/pages/$id") {
            if (includeBody) {
                parameter("body-format", "storage")
            }
            if (includeLabels) {
                parameter("include-labels", "true")
            }
            if (includeProperties) {
                parameter("include-properties", "true")
            }
        }.readApiResponse<ConfCloudPage>(expectSuccess = true)
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
        title = page.title,
        labels = page.labels?.results?.map { it.toDataModel() },
        properties = page.properties?.results?.let { results ->
            results.associate {
                it.key to it.toDataModel()
            }
        },
        body = page.body,
        version = page.version?.let {
            PageVersionInfo(
                number = it.number,
                minorEdit = it.minorEdit,
                createdAt = it.createdAt
            )
        },
        parentId = page.parentId,
        space = space,
        links = page.links
    )

    internal suspend fun getPageAttachments(pageId: String): PageAttachments {
        val attachments = httpClient.get("$apiBase/pages/$pageId/attachments").readApiResponse<CloudPageAttachments>()
        return PageAttachments(
            results = attachments.results.map {
                Attachment(
                    it.id, it.title, metadata = mapOf(
                        "comment" to it.comment,
                    ), links = it.links
                )
            },
            links = attachments.links
        )
    }

    private suspend fun resolveSpace(key: String): Space {
        val space = spacesCache.values.firstOrNull { it.key == key }
        return space ?: this.describeSpace(key, includeHome = false)
    }

    override suspend fun changeParent(
        pageId: String,
        title: String,
        version: Int,
        newParentId: String,
        updateParameters: PageUpdateOptions
    ): ConfluencePage =
        performPageUpdate(
            pageId, mapOf(
                "id" to pageId,
                "status" to "current",
                "title" to title,
                "parentId" to newParentId,
                "version" to versionNode(version, updateParameters)
            )
        )

    internal suspend fun getPageLabels(pageId: String): List<Label> {
        return httpClient.get("$apiBase/pages/$pageId/labels") {
            parameter("limit", PAGE_SIZE)
        }
            .readApiResponse<AttributesCollection<CloudPageLabel>>()
            .results.map { it.toDataModel() }
    }

    internal suspend fun getPageProperties(pageId: String): Map<String, PageProperty> {
        return httpClient.get("$apiBase/pages/$pageId/properties") {
            parameter("limit", PAGE_SIZE)
        }
            .readApiResponse<AttributesCollection<CloudPageProperty>>()
            .results.associate { it.key to it.toDataModel() }
    }

    private suspend fun performPageUpdate(pageId: String, body: Map<String, Any?>): ConfluencePage {
        val response = httpClient.put("$apiBase/pages/$pageId") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        if (response.status.isSuccess()) {
            val page = response.readApiResponse<ConfCloudPage>()
            return toConfluencePage(page, spacesCache[page.spaceId])
        } else {
            throw RuntimeException("Failed to update $pageId: ${response.bodyAsText()}")
        }
    }

    private fun versionNode(
        version: Int,
        pageUpdateOptions: PageUpdateOptions
    ): Map<String, Any> = buildMap {
        put("number", version)
        pageUpdateOptions.message?.let { put("message", it) }
    }

    private suspend inline fun <reified T> HttpResponse.readApiResponse(expectSuccess: Boolean = false): T =
        readApiResponse(expectSuccess) {
            parseCloudErrorAndThrow()
        }


    private suspend fun HttpResponse.parseCloudErrorAndThrow(): ConfluenceApiErrorException {
        val content = body<ErrorResponse>()
        val firstError = content.errors.first()
        val msg = "${firstError.code}: ${firstError.title}"
        return ConfluenceApiErrorException(status.value, msg, mapOf("detail" to firstError.detail))
    }

    private suspend fun delete(urlString: String) {
        val response = httpClient.delete(urlString)
        if (response.status == HttpStatusCode.NoContent) {
            logger.debug { "Successfully deleted resource at $urlString" }
            return
        } else {
            response.readApiResponse<String>(expectSuccess = true)
        }
    }

    internal fun registerSpaceDetails(space: Space) {
        spacesCache[space.id] = space
    }

}

private data class ErrorResponse(val errors: List<ConfluenceError>)

private data class ConfluenceError(val status: Int, val code: String, val title: String, val detail: String?)

private data class ConfCloudPage(
    val id: String,
    val status: String,
    val title: String,
    val parentId: String?,
    val version: CloudPageVersion? = null,
    val properties: AttributesCollection<CloudPageProperty>? = null,
    val labels: AttributesCollection<CloudPageLabel>? = null,
    val body: PageBody? = null,
    @JsonProperty("_links")
    val links: Map<String, String> = emptyMap(),
    val spaceId: Int
)

private data class AttributesCollection<T>(
    val results: List<T>, val meta: OptionalFieldMeta?, @JsonProperty("_links") val links: Map<String, String>
)

private data class CloudPageVersion(
    val number: Int,
    val message: String?,
    val minorEdit: Boolean,
    val createdAt: ZonedDateTime,
)

private data class CloudPageLabel(
    val prefix: String, val name: String, val id: String
) {
    fun toDataModel() = Label(prefix, name, id)
}

private data class CloudPageProperty(
    val id: String, val key: String, val value: Any?, val version: CloudVersion
) {
    fun toDataModel() = PageProperty(
        id, key, value, PropertyVersion(
            version.number
        )
    )
}

private data class OptionalFieldMeta(val hasMore: Boolean, val cursor: String?)

private data class CloudVersion(
    val createdAt: ZonedDateTime,
    val message: String,
    val number: Int,
    val minorEdit: Boolean,
    val authorId: String
)

private data class CloudPageAttachments(
    val results: List<CloudAttachment>,
    val meta: OptionalFieldMeta?,
    @JsonProperty("_links") val links: Map<String, String>
)

private data class ConfCloudPageSearchResult(
    val results: List<ConfCloudPage>, @JsonProperty("_links") val links: Map<String, String>
)

private data class PageChildItem(
    val id: String,
    val type: String,
    val title: String?,
    val status: String?,
)

private data class CloudAttachment(
    val id: String,
    val title: String,
    val comment: String?,
    @JsonProperty("_links")
    val links: Map<String, String> = emptyMap()
)