package com.github.zeldigas.confclient

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.zeldigas.confclient.model.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.ContentType
import io.ktor.serialization.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import io.ktor.utils.io.streams.*
import java.nio.file.Path
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

    override suspend fun describeSpace(key: String, includeHome: Boolean): Space {
        return httpClient.get("$apiBase/space/$key") {
            if (includeHome) {
                addExpansions(listOf("homepage"))
            }
        }.readApiResponse()
    }

    override suspend fun getPage(
        space: String,
        title: String,
        expansions: Set<PageLoadOptions>
    ): ConfluencePage {
        val results = findPages(space, title, expansions = toExpansions(expansions).toSet())

        return extractSinglePage(results)
    }

    override suspend fun getPageById(
        id: String,
        loadOptions: Set<PageLoadOptions>
    ): ConfluencePage {
        return toPageModel(httpClient.get("$apiBase/content/$id") {
            addExpansions(toExpansions(loadOptions))
        }.readApiResponse())
    }

    private fun toExpansions(loadOptions: Set<PageLoadOptions>) = buildList {
        if (SimplePageLoadOptions.Labels in loadOptions) {
            add("metadata.labels")
        }
        if (SimplePageLoadOptions.Space in loadOptions) add("space")
        if (SimplePageLoadOptions.Content in loadOptions) add("body.storage")
        if (SimplePageLoadOptions.Attachments in loadOptions) add("children.attachment")
        if (SimplePageLoadOptions.ParentId in loadOptions) add("ancestors")
        if (SimplePageLoadOptions.Version in loadOptions) add("version")

        loadOptions.filterIsInstance<PagePropertyLoad>().forEach {
            add("metadata.properties.${it.name}")
        }
    }

    override suspend fun getPageOrNull(
        space: String,
        title: String,
        loadOptions: Set<PageLoadOptions>
    ): ConfluencePage? = getPageOrNullWithExpansion(space, title, toExpansions(loadOptions).toSet())

    private suspend fun getPageOrNullWithExpansion(
        space: String,
        title: String,
        expansions: Set<String>
    ): ConfluencePage? {
        val results = findPages(space, title, expansions = expansions)

        return if (results.isEmpty()) {
            null
        } else {
            extractSinglePage(results)
        }
    }

    private suspend fun findPages(
        space: String?,
        title: String,
        expansions: Set<String>
    ): List<ConfluencePage> {
        val result: PageSearchResult = httpClient.get("$apiBase/content") {
            space?.let { parameter("spaceKey", it) }
            parameter("title", title)
            addExpansions(expansions)
        }.readApiResponse()
        return result.results.map { toPageModel(it) }
    }

    private fun HttpRequestBuilder.addExpansions(expansions: Collection<String>) {
        if (expansions.isNotEmpty()) {
            parameter("expand", expansions.joinToString(","))
        }
    }

    override suspend fun createPage(
        value: PageContentInput,
        updateParameters: PageUpdateOptions,
        loadOptions: Set<PageLoadOptions>
    ): ConfluencePage = createPageWithExpansions(
        value, updateParameters, toExpansions(loadOptions)
    )

    suspend fun createPageWithExpansions(
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
            toPageModel(response.body())
        } catch (e: ContentConvertException) {
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

    override suspend fun renamePage(
        serverPage: ConfluencePage,
        newTitle: String,
        updateParameters: PageUpdateOptions
    ): ConfluencePage =
        performPageUpdate(
            serverPage.id, mapOf(
                "type" to "page",
                "title" to newTitle,
                "version" to versionNode(serverPage.version!!.number + 1, updateParameters)
            )
        )


    private suspend fun performPageUpdate(pageId: String, body: Map<String, Any?>): ConfluencePage {
        val response = httpClient.put("$apiBase/content/$pageId") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        if (response.status.isSuccess()) {
            return toPageModel(response.readApiResponse())
        } else {
            throw RuntimeException("Failed to update $pageId: ${response.bodyAsText()}")
        }
    }

    private fun toPageModel(serverPage: ConfServerPage): ConfluencePage = ConfluencePage(
        id = serverPage.id,
        title = serverPage.title,
        labels = serverPage.metadata?.labels?.results,
        properties = serverPage.metadata?.properties,
        attachments = serverPage.children?.attachment,
        version = serverPage.version,
        body = serverPage.body,
        links = serverPage.links,
        space = serverPage.space,
        parentId = serverPage.ancestors?.lastOrNull()?.id
    )

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
        }.readApiResponse()
    }

    override suspend fun findChildPages(
        pageId: String,
        loadOptions: Set<PageLoadOptions>?
    ): List<ConfluencePage> {
        return findChildPagesWithExpansion(
            pageId,
            loadOptions?.let { toExpansions(it) }
        )
    }

    private suspend fun findChildPagesWithExpansion(pageId: String, expansions: List<String>?): List<ConfluencePage> {
        val result = mutableListOf<ConfluencePage>()
        var start = 0
        var limit = PAGE_SIZE
        var completed: Boolean
        do {
            val page = httpClient.get("$apiBase/content/$pageId/child/page") {
                addExpansions(expansions ?: emptyList())
                parameter("start", start)
                parameter("limit", limit)
            }.readApiResponse<PageSearchResult>()
            page.results.forEach { result.add(toPageModel(it)) }
            limit = page.limit
            start += limit
            completed = page.size != page.limit
        } while (!completed)
        return result
    }

    override suspend fun deletePage(pageId: String) {
        delete("$apiBase/content/$pageId")
    }

    override suspend fun deleteLabel(pageId: String, label: String) {
        delete("$apiBase/content/$pageId/label/$label")
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
                current = httpClient.get(nextPage).readApiResponse()
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
        }.readApiResponse()
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
        }.readApiResponse()
    }

    override suspend fun deleteAttachment(attachmentId: String) {
        delete("$apiBase/content/$attachmentId")
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

    override suspend fun getUserByKey(userKey: String): User {
        return httpClient.get("$apiBase/user") {
            parameter("key", userKey)
        }.readApiResponse<User>(expectSuccess = true)
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
}

private suspend inline fun <reified T> HttpResponse.readApiResponse(expectSuccess: Boolean = false): T {
    if (expectSuccess && !status.isSuccess()) {
        parseAndThrowConfluencError()
    }
    val contentType = contentType()
    if (contentType != null && (ContentType.Application.Json.match(contentType) || T::class == String::class)) {
        try {
            return body<T>()
        } catch (e: JsonConvertException) {
            parseAndThrowConfluencError()
        }
    } else {
        throw UnknownConfluenceErrorException(status.value, bodyAsText())
    }
}

private suspend fun HttpResponse.parseAndThrowConfluencError(): Nothing {
    val content = body<Map<String, Any?>>()
    throw ConfluenceApiErrorException(status.value, content["error"]?.toString() ?: "", content)
}

private data class ConfServerPage(
    val id: String,
    val type: com.github.zeldigas.confclient.ContentType,
    val status: String,
    val title: String,
    val metadata: PageMetadata? = null,
    val body: PageBody? = null,
    val version: PageVersionInfo? = null,
    val children: PageChildren? = null,
    val ancestors: List<ConfServerPage>? = null,
    val space: Space? = null,
    @JsonProperty("_links")
    val links: Map<String, String> = emptyMap()
) {
    fun pageProperty(name: String): PageProperty? {
        return metadata?.properties?.get(name)
    }
}

private data class PageMetadata(
    val labels: PageLabels?,
    @JsonIgnoreProperties("_links", "_expandable")
    val properties: Map<String, PageProperty> = emptyMap()
)

private data class PageLabels(
    val results: List<Label>,
    val size: Int
)

private enum class ContentType {
    page, blogpost
}

private data class PageSearchResult(
    val results: List<ConfServerPage>,
    val start: Int,
    val limit: Int,
    val size: Int
)