package com.github.zeldigas.confclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import com.github.zeldigas.confclient.model.Attachment
import com.github.zeldigas.confclient.model.PageAttachments
import io.ktor.http.*
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

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
}

private fun ResponseDefinitionBuilder.withJson(data: Any): ResponseDefinitionBuilder? {
    return withBody(jacksonObjectMapper().writeValueAsBytes(data))
        .withHeader("content-type", "application/json")
}
