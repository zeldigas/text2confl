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

class ConfluenceCloudClient(
    override val confluenceBaseUrl: Url,
    private val apiBase: String,
    private val httpClient: HttpClient,
    private val fallbackClient: ConfluenceClient
) : ConfluenceClient by fallbackClient {

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
        return if (includeHome && space.homepageId != null) {
            space.copy(homepage = this.getPageById(space.homepageId))
        } else {
            space
        }
    }

    private suspend fun getPageById(id: String,
                                    includeBody: Boolean = false,
                                    includeLabels: Boolean = false,
                                    includeProperties: Boolean = false): ConfluencePage {
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

        return ConfluencePage(
            id = page.id,
            type = com.github.zeldigas.confclient.model.ContentType.page,
            status = page.status,
            title = page.title,
            metadata = page.pageMetadata,
            body = page.body,
            version = page.version?.let{ PageVersionInfo(number=it.number, minorEdit = it.minorEdit, createdAt = it.createdAt) },
            children = null,
            ancestors = null
        )
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
    val links: Map<String, String> = emptyMap()
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