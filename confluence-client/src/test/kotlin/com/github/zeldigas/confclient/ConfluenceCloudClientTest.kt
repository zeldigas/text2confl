package com.github.zeldigas.confclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import com.github.zeldigas.confclient.model.*
import com.github.zeldigas.confclient.model.ContentType
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@WireMockTest
class ConfluenceCloudClientTest(runtimeInfo: WireMockRuntimeInfo) {

    private val client = confluenceClientV2(
        ConfluenceClientConfig(
            server = Url(runtimeInfo.httpBaseUrl), true, TokenAuth("testToken")
        )
    )

    @Test
    fun `Space load by key no homepage`() = runTest {
        stubFor(
            get("/api/v2/spaces?keys=Docs").willReturn(
                ok().withJsonFromFile("/data/responses/api-v2/spaces.json")
            )
        )

        val result = client.describeSpace("Docs", includeHome = false)

        assertThat(result).isEqualTo(Space(id = 98445, "Docs", "Documentation", "98581", null))
    }

    @Test
    fun `Space load by key with homepage`() = runTest {
        stubFor(
            get("/api/v2/spaces?keys=Docs").willReturn(
                ok().withJsonFromFile("/data/responses/api-v2/spaces.json")
            )
        )
        stubFor(
            get("/api/v2/pages/98581").willReturn(
                ok().withJsonFromFile("/data/responses/api-v2/space-home.json")
            )
        )

        val result = client.describeSpace("Docs", includeHome = true)

        assertThat(result).isEqualTo(
            Space(
                id = 98445, "Docs", "Documentation", "98581", ConfluencePage(
                    id = "98581",
                    type = ContentType.page,
                    status = "current",
                    title = "Docs",
                    metadata = null,
                    body = PageBody(null),
                    version = PageVersionInfo(
                        number = 1,
                        minorEdit = false,
                        createdAt = ZonedDateTime.parse("2025-03-01T15:25:51.008Z")
                    ),
                    children = null,
                    ancestors = null,
                    links = mapOf(
                        "editui" to "/pages/resumedraft.action?draftId=98581",
                        "webui" to "/spaces/Docs/overview",
                        "edituiv2" to "/spaces/Docs/pages/edit-v2/98581",
                        "tinyui" to "/x/FYEB",
                        "base" to "https://text2confl.atlassian.net/wiki"
                    )
                )
            )
        )
    }
}
