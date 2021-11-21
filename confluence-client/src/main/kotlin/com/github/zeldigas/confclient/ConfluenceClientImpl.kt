package com.github.zeldigas.confclient

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.zeldigas.confclient.model.ConfluencePage
import com.github.zeldigas.confclient.model.Space
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.http.*

class ConfluenceClientImpl(
    private val apiBase:String,
    private val httpClient: HttpClient
) : ConfluenceClient {
    override suspend fun describeSpace(key: String, expansions: List<String>): Space {
        return httpClient.get("$apiBase/space/TR") {
            addExpansions(expansions)
        }
    }

    override suspend fun getPage(
        space: String,
        title: String,
        status: List<String>?,
        expansions: List<String>
    ): ConfluencePage {
        val results = findPages(space, title, status, expansions)

        return extractSinglePage(results)
    }

    override suspend fun getPageOrNull(
        space: String,
        title: String,
        status: List<String>?,
        expansions: List<String>
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
        expansions: List<String>
    ): List<ConfluencePage> {
        val result:PageSearchResult = httpClient.get("$apiBase/content") {
            space?.let { parameter("spaceKey", it) }
            parameter("title", title)
            status?.let { parameter("status", it.toString()) }
            addExpansions(expansions)
        }
        return result.results
    }

    private fun HttpRequestBuilder.addExpansions(expansions: List<String>) {
        if (expansions.isNotEmpty()) {
            parameter("expand", expansions.joinToString(","))
        }
    }

    override suspend fun createPage(value: PageContentInput, updateParameters: UpdateParameters): ConfluencePage {
        if (value.space.isNullOrEmpty()) {
            throw IllegalArgumentException("Space is required when creating pages")
        }
        return httpClient.post("$apiBase/content") {
            contentType(ContentType.Application.Json)
            body = toPageData(value, updateParameters)
        }
    }

    override suspend fun updatePage(pageId: String, value: PageContentInput, updateParameters: UpdateParameters): ConfluencePage {
        return httpClient.put("$apiBase/content/$pageId") {
            contentType(ContentType.Application.Json)
            body = toPageData(value, updateParameters)
        }
    }

    private fun toPageData(
        value: PageContentInput,
        updateParameters: UpdateParameters
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
            put("version", buildMap {
                put("number", value.version)
                put("minorEdit", !updateParameters.notifyWatchers)
                updateParameters.message?.let { put("message", it) }
            })
            if (value.space != null) {
                put("space", mapOf("key" to value.space))
            }
        }
    }

    override suspend fun setPageProperty(pageId: String, name: String, value: PagePropertyInput) {
        return httpClient.put("$apiBase/content/$pageId/property/$name") {
            contentType(ContentType.Application.Json)
            body = value
        }
    }

    override suspend fun deleteLabel(pageId: String, label: String) {
        httpClient.delete<Unit>("$apiBase/content/$pageId/label/$label")
    }

    override suspend fun addLabels(pageId: String, labels: List<String>) {
        httpClient.post<Unit>("$apiBase/content/$pageId/label") {
            contentType(ContentType.Application.Json)
            body = labels.map { mapOf("name" to it) }
        }
    }
}

private data class PageSearchResult(
    val results: List<ConfluencePage>
)

fun confluenceClient(confluenceUrl:String, username:String, secret:String): ConfluenceClient {
    val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = JacksonSerializer(jacksonObjectMapper()
                .registerModule(Jdk8Module()).registerModule(JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES))
        }
        install(Auth) {
            basic {
                credentials {
                    BasicAuthCredentials(username = username, password = secret)
                }
                sendWithoutRequest { true }
            }
        }
    }

    return ConfluenceClientImpl("$confluenceUrl/rest/api", client)
}