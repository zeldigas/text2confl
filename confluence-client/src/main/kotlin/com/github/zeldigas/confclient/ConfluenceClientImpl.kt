package com.github.zeldigas.confclient

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.zeldigas.confclient.model.Attachment
import com.github.zeldigas.confclient.model.ConfluencePage
import com.github.zeldigas.confclient.model.PageAttachments
import com.github.zeldigas.confclient.model.Space
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.serialization.jackson.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import io.ktor.utils.io.streams.*
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager
import kotlin.io.path.fileSize

class ConfluenceClientImpl(
    override val confluenceBaseUrl: Url,
    private val apiBase: String,
    private val httpClient: HttpClient
) : ConfluenceClient {

    companion object {
        private const val PAGE_SIZE = 100
        private val logger = KotlinLogging.logger {}
    }

    override val confluenceApiBaseUrl: Url
        get() = Url(apiBase)

    override suspend fun describeSpace(key: String, expansions: List<String>): Space {
        return httpClient.get("$apiBase/space/$key") {
            addExpansions(expansions)
        }.body()
    }

    override suspend fun getPage(
        space: String,
        title: String,
        status: List<String>?,
        expansions: Set<String>
    ): ConfluencePage {
        val results = findPages(space, title, status, expansions)

        return extractSinglePage(results)
    }

    override suspend fun getPageById(
        id: String,
        expansions: Set<String>
    ): ConfluencePage {
        return httpClient.get("$apiBase/content/$id") {
            addExpansions(expansions)
        }.body()
    }

    override suspend fun getPageOrNull(
        space: String,
        title: String,
        status: List<String>?,
        expansions: Set<String>
    ): ConfluencePage? {
        val results = findPages(space, title, status, expansions)

        return if (results.isEmpty()) {
            null
        } else {
            extractSinglePage(results)
        }
    }

    private fun extractSinglePage(results: List<ConfluencePage>): ConfluencePage {
        if (results.isEmpty()) {
            throw PageNotFoundException()
        } else if (results.size > 1) {
            throw TooManyPagesFound(results)
        } else {
            return results.first()
        }
    }

    override suspend fun findPages(
        space: String?,
        title: String,
        status: List<String>?,
        expansions: Set<String>
    ): List<ConfluencePage> {
        val result: PageSearchResult = httpClient.get("$apiBase/content") {
            space?.let { parameter("spaceKey", it) }
            parameter("title", title)
            status?.let { parameter("status", it.toString()) }
            addExpansions(expansions)
        }.body()
        return result.results
    }

    private fun HttpRequestBuilder.addExpansions(expansions: Collection<String>) {
        if (expansions.isNotEmpty()) {
            parameter("expand", expansions.joinToString(","))
        }
    }

    override suspend fun createPage(
        value: PageContentInput,
        updateParameters: PageUpdateOptions,
        expansions: List<String>?
    ): ConfluencePage {
        if (value.space.isNullOrEmpty()) {
            throw IllegalArgumentException("Space is required when creating pages")
        }
        val response = httpClient.post("$apiBase/content") {
            if (expansions != null) {
                addExpansions(expansions)
            }
            contentType(ContentType.Application.Json)
            setBody(toPageData(value, updateParameters))
        }
        return try {
            response.body()
        }catch (e: ContentConvertException) {
            throw PageNotCreatedException(value.title, response.status.value, response.bodyAsText())
        }
    }

    override suspend fun updatePage(
        pageId: String,
        value: PageContentInput,
        updateParameters: PageUpdateOptions
    ): ConfluencePage {
        return performPageUpdate(pageId, toPageData(value, updateParameters))
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
                "type" to "page",
                "title" to title,
                "ancestors" to listOf(mapOf("id" to newParentId)),
                "version" to versionNode(version, updateParameters)
            )
        )

    private suspend fun performPageUpdate(pageId: String, body: Map<String, Any?>): ConfluencePage {
        val response = httpClient.put("$apiBase/content/$pageId") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw RuntimeException("Failed to update $pageId: ${response.bodyAsText()}")
        }
    }

    private fun toPageData(
        value: PageContentInput,
        pageUpdateOptions: PageUpdateOptions
    ): Map<String, Any?> {
        return buildMap {
            put("type", "page")
            value.parentPage?.let { put("ancestors", listOf(mapOf("id" to it))) }
            put("title", value.title)
            put(
                "body", mapOf(
                    "storage" to mapOf(
                        "value" to value.content,
                        "representation" to "storage"
                    )
                )
            )
            put("version", versionNode(value.version, pageUpdateOptions))
            if (value.space != null) {
                put("space", mapOf("key" to value.space))
            }
        }
    }

    private fun versionNode(
        version: Int,
        pageUpdateOptions: PageUpdateOptions
    ): Map<String, Any> = buildMap {
        put("number", version)
        put("minorEdit", !pageUpdateOptions.notifyWatchers)
        pageUpdateOptions.message?.let { put("message", it) }
    }

    override suspend fun setPageProperty(pageId: String, name: String, value: PagePropertyInput) {
        return httpClient.put("$apiBase/content/$pageId/property/$name") {
            contentType(ContentType.Application.Json)
            setBody(value)
        }.body()
    }

    override suspend fun findChildPages(pageId: String, expansions: List<String>?): List<ConfluencePage> {
        val result = mutableListOf<ConfluencePage>()
        var start = 0
        var limit = PAGE_SIZE
        var completed: Boolean
        do {
            val page = httpClient.get("$apiBase/content/$pageId/child/page") {
                addExpansions(expansions ?: emptyList())
                parameter("start", start)
                parameter("limit", limit)
            }.body<PageSearchResult>()
            result.addAll(page.results)
            limit = page.limit
            start += limit
            completed = page.size != page.limit
        } while (!completed)
        return result
    }

    override suspend fun deletePage(pageId: String) {
        httpClient.delete("$apiBase/content/$pageId")
    }

    override suspend fun deleteLabel(pageId: String, label: String) {
        httpClient.delete("$apiBase/content/$pageId/label/$label")
    }

    override suspend fun addLabels(pageId: String, labels: List<String>) {
        httpClient.post("$apiBase/content/$pageId/label") {
            contentType(ContentType.Application.Json)
            setBody(labels.map { mapOf("name" to it) })
        }
    }

    override suspend fun fetchAllAttachments(pageAttachments: PageAttachments): List<Attachment> {
        return buildList {
            addAll(pageAttachments.results)
            var current = pageAttachments
            while ("next" in current.links) {
                val nextPage = makeLink(confluenceBaseUrl, current.links.getValue("next"))
                logger.debug { "Loading next attachments page: $nextPage" }
                current = httpClient.get(nextPage).body()
                if (current.results.isEmpty()) {
                    break
                } else {
                    addAll(current.results)
                }
            }
        }
    }

    override suspend fun addAttachments(
        pageId: String,
        pageAttachmentInput: List<PageAttachmentInput>
    ): PageAttachments {
        return httpClient.submitFormWithBinaryData("$apiBase/content/$pageId/child/attachment", formData {
            for (attachment in pageAttachmentInput) {
                addAttachmentToForm(attachment)
            }
        }) {
            header("X-Atlassian-Token", "nocheck")
            header("Accept", "application/json")
        }.body()
    }

    override suspend fun updateAttachment(
        pageId: String,
        attachmentId: String,
        pageAttachmentInput: PageAttachmentInput
    ): Attachment {
        return httpClient.submitFormWithBinaryData(
            "$apiBase/content/$pageId/child/attachment/$attachmentId/data",
            formData {
                addAttachmentToForm(pageAttachmentInput)
            }) {
            header("X-Atlassian-Token", "nocheck")
            header("Accept", "application/json")
        }.body()
    }

    override suspend fun deleteAttachment(attachmentId: String) {
        httpClient.delete("$apiBase/content/$attachmentId").body<String>()
    }

    override suspend fun downloadAttachment(attachment: Attachment, destination: Path) {
        val downloadLink = attachment.links["download"]
            ?: throw IllegalArgumentException("No download link found: ${attachment.links}")

        val response = httpClient.get(makeLink(confluenceBaseUrl, downloadLink))

        response.bodyAsChannel().copyAndClose(destination.toFile().writeChannel())
    }

    private fun FormBuilder.addAttachmentToForm(attachment: PageAttachmentInput) {
        append("comment", attachment.comment ?: "")
        append(
            "file",
            InputProvider(attachment.content.fileSize()) { attachment.content.toFile().inputStream().asInput() },
            Headers.build {
                attachment.contentType?.let { append(HttpHeaders.ContentType, it) }
                append(HttpHeaders.ContentDisposition, "filename=${attachment.name}")
            })
    }
}

private data class PageSearchResult(
    val results: List<ConfluencePage>,
    val start: Int,
    val limit: Int,
    val size: Int
)

fun confluenceClient(
    config: ConfluenceClientConfig
): ConfluenceClient {
    val client = HttpClient(CIO) {
        if (config.skipSsl) {
            engine {
                https {
                    trustManager = object : X509TrustManager {
                        override fun getAcceptedIssuers(): Array<X509Certificate>? = null
                        override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
                        override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
                    }
                }
            }
        }
        install(ContentNegotiation) {
            jackson {
                registerModule(Jdk8Module())
                registerModule(JavaTimeModule())
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            }
        }

        install(Auth) {
            config.auth.create(this)
        }

        install(UserAgent) {
            agent = "text2confl"
        }
    }

    return ConfluenceClientImpl(config.server, "${config.server}/rest/api", client)
}