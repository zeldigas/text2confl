package com.github.zeldigas.confclient

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.zeldigas.confclient.model.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import java.nio.file.Path
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

private val logger = KotlinLogging.logger {}

interface ConfluenceClient {

    val confluenceBaseUrl: Url
    val confluenceApiBaseUrl: Url

    suspend fun describeSpace(key: String, includeHome: Boolean = true): Space

    suspend fun getPageById(id: String, loadOptions: Set<PageLoadOptions>): ConfluencePage

    suspend fun getPage(
        space: String, title: String,
        loadOptions: Set<PageLoadOptions> = emptySet()
    ): ConfluencePage

    suspend fun getPageOrNull(
        space: String, title: String,
        loadOptions: Set<PageLoadOptions> = emptySet()
    ): ConfluencePage?

    suspend fun createPage(
        value: PageContentInput,
        updateParameters: PageUpdateOptions,
        loadOptions: Set<PageLoadOptions> = emptySet()
    ): ConfluencePage

    suspend fun updatePage(pageId: String, value: PageContentInput, updateParameters: PageUpdateOptions): ConfluencePage

    suspend fun renamePage(serverPage: ConfluencePage, newTitle: String, updateParameters: PageUpdateOptions) : ConfluencePage

    suspend fun changeParent(
        pageId: String,
        title: String,
        version: Int,
        newParentId: String,
        updateParameters: PageUpdateOptions
    ): ConfluencePage

    suspend fun createPageProperty(pageId: String, name: String, value: PagePropertyInput)

    suspend fun updatePageProperty(pageId: String, property: PageProperty, value: PagePropertyInput)

    suspend fun findChildPages(pageId: String, loadOptions: Set<PageLoadOptions>? = null): List<ConfluencePage>

    suspend fun deletePage(pageId: String)

    suspend fun deleteLabel(pageId: String, label: String)

    suspend fun addLabels(pageId: String, labels: List<String>)

    suspend fun fetchAllAttachments(pageAttachments: PageAttachments): List<Attachment>

    suspend fun addAttachments(pageId: String, pageAttachmentInput: List<PageAttachmentInput>): PageAttachments

    suspend fun updateAttachment(
        pageId: String,
        attachmentId: String,
        pageAttachmentInput: PageAttachmentInput
    ): Attachment

    suspend fun deleteAttachment(attachmentId: String)

    suspend fun downloadAttachment(attachment: Attachment, destination: Path)

    suspend fun getUserByKey(userKey: String): User

}

sealed interface PageLoadOptions

enum class SimplePageLoadOptions : PageLoadOptions {
    Space, Content, Labels, Attachments, ParentId, Version
}

data class PagePropertyLoad(val name: String) : PageLoadOptions

class SpaceNotFoundException(val space: String): RuntimeException()

class PageNotCreatedException(val title: String, val status: Int, val body: String?) :
    RuntimeException("Failed to create '$title' page: status=$status, body:\n$body")

class PageNotFoundException : RuntimeException()

class TooManyPagesFound(val pages: List<ConfluencePage>) : RuntimeException()

class UnknownConfluenceErrorException(val status: Int, val body: String?) :
    RuntimeException("Unknown Confluence error: status=$status, body:\n$body")

class ConfluenceApiErrorException(val status: Int, val error: String, val body: Map<String, Any?>) :
    RuntimeException("Confluence API error: status=$error, body:\n$body")


internal fun extractSinglePage(results: List<ConfluencePage>): ConfluencePage {
    if (results.isEmpty()) {
        throw PageNotFoundException()
    } else if (results.size > 1) {
        throw TooManyPagesFound(results)
    } else {
        return results.first()
    }
}


private fun httpClientForApi(config: ConfluenceClientConfig) = HttpClient(CIO) {
    engine {
        if (config.requestTimeout != null) {
            requestTimeout = config.requestTimeout
        }
        if (config.skipSsl) {
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
    if (config.httpLogLevel != LogLevel.NONE) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = config.httpLogLevel
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
    }
}

fun confluenceClient(
    config: ConfluenceClientConfig
): ConfluenceClient {
    val client = httpClientForApi(config)
    return confluenceClientV1(config, client)
}

private fun confluenceClientV1(
    config: ConfluenceClientConfig,
    client: HttpClient
): ConfluenceClientImpl {
    val baseUrl = URLBuilder(config.server).appendPathSegments("rest", "api").build().toString()
    return ConfluenceClientImpl(config.server, baseUrl, client)
}

fun confluenceClientV2(
    config: ConfluenceClientConfig
): ConfluenceClient {
    val client = httpClientForApi(config)
    client.plugin(HttpSend).intercept { request ->
        val call = execute(request)
        if ("Warning" in call.response.headers) {
            logger.warn { "${call.response.headers["Warning"]}: ${request.method} ${request.url}" }
        }
        call
    }
    val baseUrl = URLBuilder(config.server).appendPathSegments("api", "v2").build().toString()
    return ConfluenceCloudClient(
        config.server, baseUrl, client, confluenceClientV1(config, client)
    )
}