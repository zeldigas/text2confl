package com.github.zeldigas.confclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import com.github.zeldigas.confclient.model.*
import io.ktor.http.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@WireMockTest
class ConfluenceCloudClientTest(runtimeInfo: WireMockRuntimeInfo) {

    private val client = confluenceClientV2(
        ConfluenceClientConfig(
            server = Url(runtimeInfo.httpBaseUrl), true, TokenAuth("testToken")
        )
    ) as ConfluenceCloudClient

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
                    title = "Docs",
                    body = PageBody(null),
                    version = PageVersionInfo(
                        number = 1,
                        minorEdit = false,
                        createdAt = ZonedDateTime.parse("2025-03-01T15:25:51.008Z")
                    ),
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

    @Test
    fun `Load page labels`() = runTest {
        stubFor(
            get("/api/v2/pages/123/labels?limit=100").willReturn(
                ok().withJsonFromFile("/data/responses/api-v2/page-labels.json")
            )
        )

        val result = client.getPageLabels("123")

        assertThat(result).isEqualTo(
            listOf(
                Label("global", "aaa", "131075"),
                Label("global", "abc", "524289")
            )
        )
    }

    @Test
    fun `Load page properties`() = runTest {
        stubFor(
            get("/api/v2/pages/123/properties?limit=100").willReturn(
                ok().withJsonFromFile("/data/responses/api-v2/page-properties.json")
            )
        )

        val result = client.getPageProperties("123")

        assertThat(result).isEqualTo(
            mapOf(
                "content-appearance-draft" to PageProperty(
                    "1048577",
                    "content-appearance-draft",
                    "full-width",
                    PropertyVersion(1)
                ),
                "content-appearance-published" to PageProperty(
                    "1081353",
                    "content-appearance-published",
                    "full-width",
                    PropertyVersion(1)
                )
            )
        )
    }

    @Test
    fun `Load page attachments`() = runTest {
        stubFor(
            get("/api/v2/pages/123/attachments").willReturn(
                ok().withJsonFromFile("/data/responses/api-v2/page-attachments.json")
            )
        )

        val result = client.getPageAttachments("123")

        assertThat(result).isEqualTo(
            PageAttachments(
                results = listOf(
                    Attachment(
                        id = "att1441849",
                        title = "test.txt",
                        metadata = mapOf("comment" to "HASH:ad0289848913e74b3cddda83e68c1b434da5a06cf3924c3d3ee83a3feaa94ac2"),
                        links = mapOf(
                            "download" to "/download/attachments/1507344/test.txt?version=5&modificationDate=1763499437434&cacheVersion=1&api=v2",
                            "webui" to "/pages/viewpageattachments.action?pageId=1507344&preview=%2F1507344%2F1441849%2Ftest.txt"
                        )
                    )
                ),
                links = mapOf(
                    "base" to "https://text2conf.atlassian.net/wiki"
                )
            )
        )
    }

    @Test
    fun `Rename page`() = runTest {
        stubFor(
            put("/api/v2/pages/1441795/title")
                .willReturn(ok().withJsonFromFile("/data/responses/api-v2/page-after-rename.json"))
        )

        val result = client.renamePage(mockk {
            every { id } returns "1441795"
        }, "doc title subtitle", PageUpdateOptions(message = null))

        assertThat(result).isEqualTo(
            ConfluencePage(
                id = "1441795",
                title = "doc title subtitle",
                version = PageVersionInfo(9, false, createdAt = ZonedDateTime.parse("2025-11-19T05:44:33.959Z")),
                links = mapOf(
                    "editui" to "/pages/resumedraft.action?draftId=1441795",
                    "webui" to "/spaces/docs/pages/1441795/doc+title+subtitle",
                    "edituiv2" to "/spaces/docs/pages/edit-v2/1441795",
                    "tinyui" to "/x/AwAW",
                    "base" to "https://text2conf.atlassian.net/wiki"
                )
            )
        )
    }
}
