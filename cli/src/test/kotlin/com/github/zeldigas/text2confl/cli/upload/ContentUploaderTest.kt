package com.github.zeldigas.text2confl.cli.upload

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isFailure
import assertk.assertions.isSuccess
import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.text2confl.cli.config.Cleanup
import com.github.zeldigas.text2confl.convert.Page
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class ContentUploaderTest(
    @MockK private val uploadOperations: PageUploadOperations,
    @MockK private val confluenceClient: ConfluenceClient
) {

    @BeforeEach
    internal fun setUp() {
        coEvery { uploadOperations.updatePageLabels(any(), any()) } just Runs
        coEvery { uploadOperations.updatePageAttachments(any(), any()) } just Runs
    }

    @Test
    internal fun `Upload pages hierarchy`() {
        val contentUploader = contentUploader()
        val registry = mutableMapOf<Page, Long>()
        val fastPage = aPage(1, registry) {
            every { children } returns listOf(
                aPage(100, registry) {
                    every { children } returns emptyList()
                },
                aPage(2, registry) {
                    every { children } returns emptyList()
                },
                aPage(200, registry) {
                    every { children } returns emptyList()
                }
            )
        }
        val mediumPage = aPage(50, registry) {
            every { children } returns emptyList()
        }

        val slowPage = aPage(500, registry) {
            every { children } returns emptyList()
        }

        registry.forEach { (page, delayTime) ->
            coEvery { uploadOperations.createOrUpdatePageContent(page, "TEST", any()) } coAnswers {
                delay(delayTime)
                mockk() {
                    every { id } returns "$delayTime"
                }
            }
        }

        runBlocking { contentUploader.uploadPages(listOf(fastPage, mediumPage, slowPage), "TEST", "id") }

        listOf(fastPage, mediumPage, slowPage).forEach {
            coVerify { uploadOperations.createOrUpdatePageContent(it, "TEST", "id") }
        }
        fastPage.children.forEach {
            coVerify { uploadOperations.createOrUpdatePageContent(it, "TEST", "1") }
        }
    }

    private fun contentUploader(cleanup: Cleanup = Cleanup.None) = ContentUploader(
        uploadOperations,
        confluenceClient,
        cleanup
    )

    @Test
    internal fun `Failed upload abort others`() {
        val contentUploader = contentUploader()
        val fastPage = aPage("Fast page") {
            every { children } returns emptyList()
        }
        val failedPage = aPage("Failed page")
        val slowPage = aPage("Slow page")

        val fastServerPage = ServerPage("fastId", "id", emptyList(), emptyList())
        coEvery { uploadOperations.createOrUpdatePageContent(fastPage, "TEST", "id") } returns fastServerPage

        coEvery { uploadOperations.createOrUpdatePageContent(failedPage, any(), any()) } coAnswers {
            delay(100)
            throw RuntimeException("Upload failed")
        }

        val slowServerPage = ServerPage("slowId", "id", emptyList(), emptyList())
        coEvery { uploadOperations.createOrUpdatePageContent(slowPage, "TEST", "id") } coAnswers {
            delay(20000)
            slowServerPage
        }

        assertThat {
            runBlocking { contentUploader.uploadPages(listOf(slowPage, failedPage, fastPage), "TEST", "id") }
        }.isFailure().hasMessage("Upload failed")

        coVerify(exactly = 1) { uploadOperations.updatePageLabels(fastServerPage, any()) }
        coVerify(exactly = 0) { uploadOperations.updatePageLabels(slowServerPage, any()) }
    }

    @Test
    internal fun `Custom parent from attributes is taken into account`() {
        val contentUploader = contentUploader()
        val defaultPage = aPage("Default page") {
            every { children } returns emptyList()
        }
        val pageWithParentId = aPage("Parent id", mapOf("parentId" to 123, "parent" to "ignored")) {
            every { children } returns emptyList()
        }
        val pageWithParentTitle = aPage("Parent title", mapOf("parent" to "Custom")) {
            every { children } returns emptyList()
        }

        val defaultServerPage = ServerPage("defaultId", "id", emptyList(), emptyList())
        coEvery { uploadOperations.createOrUpdatePageContent(defaultPage, "TEST", "id") } returns defaultServerPage

        val parentIdServerPage = ServerPage("pIdPage", "123", emptyList(), emptyList())
        coEvery { uploadOperations.createOrUpdatePageContent(pageWithParentId, any(), "123") } returns parentIdServerPage

        val parentTitleServerPage = ServerPage("tIdPage", "345", emptyList(), emptyList())
        coEvery { uploadOperations.createOrUpdatePageContent(pageWithParentTitle, "TEST", "345") } returns parentTitleServerPage

        coEvery { confluenceClient.getPage("TEST", "Custom") } returns mockk { every { id } returns "345" }

        assertThat {
            runBlocking { contentUploader.uploadPages(listOf(defaultPage, pageWithParentId, pageWithParentTitle), "TEST", "id") }
        }.isSuccess()

        coVerify { uploadOperations.createOrUpdatePageContent(defaultPage, "TEST", "id") }
        coVerify { uploadOperations.createOrUpdatePageContent(pageWithParentId, "TEST", "123") }
        coVerify { uploadOperations.createOrUpdatePageContent(pageWithParentTitle, "TEST", "345") }
    }

    @Test
    internal fun `Removal of all orphans`() {
        val contentUploader = contentUploader(Cleanup.All)
        val (childPage, fastPage) = simplePageStructure()
        givenPagesCreated(fastPage, childPage)
        givenChildPagesFound()
        coEvery { uploadOperations.deletePageWithChildren(any()) } just Runs

        runBlocking { contentUploader.uploadPages(listOf(fastPage), "TEST", "id") }

        coVerify { uploadOperations.createOrUpdatePageContent(fastPage, "TEST", "id") }
        coVerify { uploadOperations.deletePageWithChildren("child1")}
        coVerify { uploadOperations.deletePageWithChildren("child2") }
        coVerify(exactly = 0) { uploadOperations.deletePageWithChildren("child3") }
    }

    private fun simplePageStructure(): Pair<Page, Page> {
        val childPage = aPage("child3") {
            every { children } returns emptyList()
        }
        val fastPage = aPage("parent") {
            every { children } returns listOf(childPage)
        }
        return Pair(childPage, fastPage)
    }

    private fun givenPagesCreated(
        fastPage: Page,
        childPage: Page
    ) {
        coEvery { uploadOperations.createOrUpdatePageContent(fastPage, any(), any()) } returns mockk {
            every { id } returns "1"
        }
        coEvery { uploadOperations.createOrUpdatePageContent(childPage, any(), any()) } returns mockk {
            every { id } returns "child3"
        }
    }

    @Test
    internal fun `Removal of managed pages`() {
        val contentUploader = contentUploader(Cleanup.Managed)
        val (childPage, fastPage) = simplePageStructure()
        givenPagesCreated(fastPage, childPage)
        givenChildPagesFound()
        coEvery { uploadOperations.deletePageWithChildren(any()) } just Runs

        runBlocking { contentUploader.uploadPages(listOf(fastPage), "TEST", "id") }

        coVerify { uploadOperations.createOrUpdatePageContent(fastPage, "TEST", "id") }
        coVerify(exactly = 0) { uploadOperations.deletePageWithChildren("child1")}
        coVerify { uploadOperations.deletePageWithChildren("child2") }
        coVerify(exactly = 0) { uploadOperations.deletePageWithChildren("child3")}
    }

    @Test
    internal fun `Removal of no pages`() {
        val contentUploader = contentUploader(Cleanup.None)
        val (childPage, fastPage) = simplePageStructure()
        givenPagesCreated(fastPage, childPage)
        givenChildPagesFound()

        runBlocking { contentUploader.uploadPages(listOf(fastPage), "TEST", "id") }

        coVerify(exactly = 0) { uploadOperations.deletePageWithChildren(any())}
    }

    private fun givenChildPagesFound() {
        coEvery { uploadOperations.findChildPages("1") } returns listOf(
            mockk {
                every { title } returns "child1"
                every { id } returns "child1"
                every { pageProperty(HASH_PROPERTY) } returns null
            },
            mockk {
                every { title } returns "child2"
                every { id } returns "child2"
                every { pageProperty(HASH_PROPERTY) } returns mockk()
            },
            mockk {
                every { title } returns "child3"
                every { id } returns "child3"
                every { pageProperty(HASH_PROPERTY) } returns mockk()
            }
        )
        coEvery { uploadOperations.findChildPages("child3") } returns emptyList()
    }

    private fun aPage(creationTime: Long, creationTimeRegistry: MutableMap<Page, Long>, attributesValues: Map<String, Any?> = emptyMap(), block: Page.() -> Unit = {}) : Page {
        val page = mockk<Page>(block = {
            every { content } returns mockk {
                every { header } returns mockk {
                    every { attributes } returns attributesValues
                }
            }
            every { title } returns "Page created for ${creationTime}ms"
            block()
        })
        creationTimeRegistry[page] = creationTime
        return page
    }

    private fun aPage(pageTitle: String, attributesValues: Map<String, Any?> = emptyMap(), block: Page.() -> Unit = {}) : Page {
        val page = mockk<Page>(block = {
            every { content } returns mockk {
                every { header } returns mockk {
                    every { attributes } returns attributesValues
                }
            }
            every { title } returns pageTitle
            block()
        })
        return page
    }
}