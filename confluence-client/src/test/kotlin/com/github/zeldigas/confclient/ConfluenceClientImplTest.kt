package com.github.zeldigas.confclient

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import com.github.zeldigas.text2confl.model.Attachment
import com.github.zeldigas.text2confl.model.PageAttachments
import com.github.zeldigas.text2confl.model.User
import io.ktor.http.*
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@WireMockTest
class ConfluenceClientImplTest(runtimeInfo: WireMockRuntimeInfo) {

    private val client = confluenceClient(
        ConfluenceClientConfig(
            server = Url(runtimeInfo.httpBaseUrl), true, TokenAuth("testToken")
        )
    )

    @Test
    fun `Fetch attachments with all in first page`() = runTest {
        val attachments = listOf<Attachment>(mockk())

        val result = client.fetchAllAttachments(PageAttachments(results = attachments))

        assertThat(result).isEqualTo(attachments)
    }

    @Test
    fun `Fetch attachments with multiple pages`() = runTest {
        val attachments = listOf(Attachment("first", "first"))
        val firstNext = "/rest/api/content/123/child/attachment?limit=1&start=1"
        val secondNext = "/rest/api/content/123/child/attachment?limit=1&start=2"
        stubFor(
            get(firstNext).willReturn(
                ok().withJson(
                    PageAttachments(
                        results = listOf(Attachment("a", "attachment a")),
                        links = mapOf("next" to secondNext)
                    )
                )
            )
        )
        stubFor(
            get(secondNext).willReturn(
                ok().withJson(
                    mapOf(
                        "results" to listOf(
                            mapOf(
                                "id" to "b", "title" to "attachment b",
                                "metadata" to mapOf("comment" to "some comment")
                            )
                        )
                    )
                )
            )
        )


        val result = client.fetchAllAttachments(
            PageAttachments(
                results = attachments,
                links = mapOf("next" to firstNext)
            )
        )

        assertThat(result).isEqualTo(
            listOf(
                attachments[0],
                Attachment("a", "attachment a"),
                Attachment("b", "attachment b", mapOf("comment" to "some comment")),
            )
        )
    }

    @Test
    fun getUserByKey() = runTest {
        stubFor(
            get("/rest/api/user?key=abc").willReturn(
                ok().withJson(
                    mapOf(
                        "type" to "known",
                        "username" to "user@example.org",
                        "userKey" to "abc",
                        "profilePicture" to mapOf(
                            "path" to "/download/attachments/123/avatar.jpg",
                            "width" to 48,
                            "height" to 48,
                            "isDefault" to false
                        ),
                        "displayName" to "User Name",
                        "_links" to mapOf(
                            "base" to "https://wiki.example.org",
                            "context" to "",
                            "self" to "https://wiki..example.org/rest/api/user?key=abc"
                        )
                    )
                )
            )
        )

        val result = client.getUserByKey("abc")

        assertThat(result).isEqualTo(User("known", "user@example.org", "abc", "User Name"))
    }

    @Test
    fun `geUserByKey not found`() = runTest {
        stubFor(
            get("/rest/api/user?key=abc").willReturn(
                notFound().withJson(
                    mapOf(
                        "statusCode" to 404,
                        "data" to mapOf(
                            "authorized" to false,
                            "valid" to true,
                        ),
                        "message" to "No user found with key : abc",
                        "reason" to "Not Found"
                    )
                )
            )
        )

        val result = assertThrows<ConfluenceApiErrorException> { client.getUserByKey("abc") }

        assertThat(result.status).isEqualTo(404)
        assertThat(result.error).isEmpty()
        assertThat(result.message).isNotNull().contains("No user found with key : abc")
    }
}

private fun ResponseDefinitionBuilder.withJson(data: Any): ResponseDefinitionBuilder? {
    return withBody(jacksonObjectMapper().writeValueAsBytes(data))
        .withHeader("content-type", "application/json")
}
