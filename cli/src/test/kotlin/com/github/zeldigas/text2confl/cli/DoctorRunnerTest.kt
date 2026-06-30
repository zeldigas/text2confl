package com.github.zeldigas.text2confl.cli

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.github.zeldigas.confclient.ConfluenceAuthorizationException
import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.confclient.PagePropertyLoad
import com.github.zeldigas.confclient.RequestDetails
import com.github.zeldigas.confclient.SimplePageLoadOptions
import com.github.zeldigas.confclient.model.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlin.io.path.writeText
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

private val noOpReporter = object : DoctorReporter {
    override fun onStep(report: StepReport) {}
}

@ExtendWith(MockKExtension::class)
internal class DoctorRunnerTest(
    @MockK private val client: ConfluenceClient
) {
    private val space = "TEST"
    private val parentId = "parent-123"
    private val pageId = "page-456"
    private val attachmentId = "att-789"
    private val propertyId = "prop-101"

    private fun runner(
        isCloud: Boolean = false,
        isScopedToken: Boolean = false,
        givenParentId: String? = parentId,
        givenParentTitle: String? = null
    ) = DoctorRunner(
        client = client,
        space = space,
        parentId = givenParentId,
        parentTitle = givenParentTitle,
        isCloud = isCloud,
        isScopedToken = isScopedToken
    )

    @BeforeEach
    fun setUp() {
        coEvery { client.describeSpace(space, includeHome = false) } returns mockk {
            every { homepageId } returns "home-1"
        }

        val parentIdValue = parentId
        val parentPageMetadata = setOf(
            SimplePageLoadOptions.Version, SimplePageLoadOptions.Content, SimplePageLoadOptions.Labels
        )
        coEvery { client.getPageById(parentIdValue, parentPageMetadata) } returns mockk {
            every { id } returns parentIdValue
        }

        coEvery { client.createPage(any(), any()) } returns mockk {
            every { id } returns pageId
            every { title } returns "text2confl test abc12345"
            every { version } returns PageVersionInfo(1, true, null)
        }

        coJustRun { client.addLabels(pageId, any()) }

        coEvery { client.getPageById(pageId, setOf(SimplePageLoadOptions.Labels)) } returns mockk {
            every { labels } returns listOf(Label("global", "text2confl-doctor", "lbl-1"))
        }

        coJustRun { client.createPageProperty(pageId, any(), any()) }

        val property = PageProperty(propertyId, "text2confl-doctor", "initial", PropertyVersion(1))
        coEvery { client.getPageById(pageId, setOf(PagePropertyLoad("text2confl-doctor"))) } returns mockk {
            every { pageProperty("text2confl-doctor") } returns property
        }

        coEvery { client.addAttachments(pageId, any()) } returns PageAttachments(
            results = listOf(Attachment(attachmentId, "text2confl-doctor.txt"))
        )

        coEvery { client.downloadAttachment(any(), any()) } coAnswers {
            val dest = secondArg<java.nio.file.Path>()
            dest.writeText("text2confl doctor test attachment")
        }

        coEvery { client.updatePage(pageId, any(), any()) } returns mockk {
            every { version } returns PageVersionInfo(2, true, null)
        }

        coEvery { client.getPageById(pageId, setOf(SimplePageLoadOptions.Version)) } returns mockk {
            every { id } returns pageId
            every { title } returns "text2confl test abc12345"
            every { version } returns PageVersionInfo(2, true, null)
        }

        coEvery { client.renamePage(any(), any(), any()) } returns mockk {
            every { title } returns "text2confl test abc12345 (renamed)"
        }

        coEvery { client.findChildPages(pageId) } returns emptyList()

        coJustRun { client.updatePageProperty(pageId, any(), any()) }

        coEvery { client.updateAttachment(pageId, attachmentId, any()) } returns mockk()

        coJustRun { client.deleteLabel(pageId, "text2confl-doctor") }
        coJustRun { client.deleteAttachment(attachmentId) }
        coJustRun { client.deletePage(pageId) }
    }

    @Test
    fun `all steps pass on happy path`() {
        val results = runBlocking { runner().run(noOpReporter) }

        assertThat(results).transform { it.size }.isEqualTo(16)
        results.forEach { report ->
            assertThat(report.result, "step '${report.name}'").isInstanceOf(StepResult.Pass::class)
        }
    }

    @Test
    fun `create-page failure skips all dependent steps`() {
        val error = RuntimeException("503 Service Unavailable")
        coEvery { client.createPage(any(), any()) } throws error

        val results = runBlocking { runner().run(noOpReporter) }

        assertThat(stepResult(results, "describe-space")).isInstanceOf(StepResult.Pass::class)
        assertThat(stepResult(results, "get-page")).isInstanceOf(StepResult.Pass::class)
        assertThat(stepResult(results, "create-page")).isInstanceOf(StepResult.Fail::class)
        listOf(
            "find-child-pages", "add-labels", "read-labels", "create-property",
            "add-attachment", "download-attachment", "update-page", "rename-page",
            "update-property", "update-attachment", "delete-label", "delete-attachment", "delete-page"
        ).forEach { name ->
            assertThat(stepResult(results, name), name).isInstanceOf(StepResult.Skipped::class)
        }
    }

    @Test
    fun `create-property failure skips update-property only`() {
        coEvery { client.createPageProperty(pageId, any(), any()) } throws RuntimeException("forbidden")

        val results = runBlocking { runner().run(noOpReporter) }

        assertThat(stepResult(results, "create-property")).isInstanceOf(StepResult.Fail::class)
        assertThat(stepResult(results, "update-property")).isInstanceOf(StepResult.Skipped::class)

        // other steps unaffected
        listOf("add-labels", "read-labels", "add-attachment", "update-page",
            "update-attachment", "delete-label", "delete-attachment", "delete-page"
        ).forEach { name ->
            assertThat(stepResult(results, name), name).isInstanceOf(StepResult.Pass::class)
        }
    }

    @Test
    fun `add-attachment failure skips update-attachment and delete-attachment`() {
        coEvery { client.addAttachments(pageId, any()) } throws RuntimeException("quota exceeded")

        val results = runBlocking { runner().run(noOpReporter) }

        assertThat(stepResult(results, "add-attachment")).isInstanceOf(StepResult.Fail::class)
        assertThat(stepResult(results, "update-attachment")).isInstanceOf(StepResult.Skipped::class)
        assertThat(stepResult(results, "delete-attachment")).isInstanceOf(StepResult.Skipped::class)

        // page deletion still happens
        assertThat(stepResult(results, "delete-page")).isInstanceOf(StepResult.Pass::class)
    }

    @Test
    fun `delete-page runs even when intermediate steps fail`() {
        coEvery { client.addAttachments(pageId, any()) } throws RuntimeException("quota")
        coEvery { client.createPageProperty(pageId, any(), any()) } throws RuntimeException("forbidden")

        val results = runBlocking { runner().run(noOpReporter) }

        assertThat(stepResult(results, "delete-page")).isInstanceOf(StepResult.Pass::class)
    }

    @Test
    fun `hint for describe-space auth error on cloud scoped token`() {
        val authError = ConfluenceAuthorizationException(
            RequestDetails("GET", "/rest/api/space/TEST"), 403, emptyMap(), "Forbidden"
        )
        coEvery { client.describeSpace(space, includeHome = false) } throws authError

        val results = runBlocking { runner(isCloud = true, isScopedToken = true).run(noOpReporter) }

        val fail = stepResult(results, "describe-space") as StepResult.Fail
        assertThat(fail).prop(StepResult.Fail::hint).transform { it.lowercase() }
            .transform { it.contains("scope") }.isEqualTo(true)
    }

    @Test
    fun `hint for create-page auth error on cloud classic token`() {
        val authError = ConfluenceAuthorizationException(
            RequestDetails("POST", "/rest/api/content"), 403, emptyMap(), "Forbidden"
        )
        coEvery { client.createPage(any(), any()) } throws authError

        val results = runBlocking { runner(isCloud = true, isScopedToken = false).run(noOpReporter) }

        val fail = stepResult(results, "create-page") as StepResult.Fail
        assertThat(fail).prop(StepResult.Fail::hint).transform { it.lowercase() }
            .transform { it.contains("permission") }.isEqualTo(true)
    }

    @Test
    fun `hint for delete-page mentions page id on non-auth failure`() {
        coEvery { client.deletePage(pageId) } throws RuntimeException("timeout")

        val results = runBlocking { runner().run(noOpReporter) }

        val fail = stepResult(results, "delete-page") as StepResult.Fail
        assertThat(fail).prop(StepResult.Fail::hint).transform { it.contains(pageId) }.isEqualTo(true)
    }

    private fun stepResult(results: List<StepReport>, name: String): StepResult =
        results.first { it.name == name }.result
}
