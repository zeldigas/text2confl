package com.github.zeldigas.text2confl.core.upload

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.text2confl.model.ConfluencePage
import com.github.zeldigas.text2confl.model.ContentType
import com.github.zeldigas.text2confl.model.PageMetadata
import com.github.zeldigas.text2confl.model.PageProperty
import com.github.zeldigas.text2confl.convert.Page
import com.github.zeldigas.text2confl.core.config.Cleanup
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.io.path.Path

@ExtendWith(MockKExtension::class)
internal class ContentUploaderTest(
    @MockK private val uploadOperations: PageUploadOperations,
    @MockK private val confluenceClient: ConfluenceClient,
    @MockK(relaxed = true) private val tracker: UploadOperationTracker,
) {

    @BeforeEach
    internal fun setUp() {
        coEvery { uploadOperations.updatePageLabels(any(), any()) } returns LabelsUpdateResult.NotChanged
        coEvery { uploadOperations.updatePageAttachments(any(), any()) } returns AttachmentsUpdateResult.NotChanged
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
                PageOperationResult.Created(page, mockk {
                    every { id } returns "$delayTime"
                })
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

    private fun contentUploader(cleanup: Cleanup = Cleanup.None, tenant: String? = null) = ContentUploader(
        uploadOperations,
        confluenceClient,
        cleanup,
        tenant,
        tracker
    )

    @Test
    internal fun `Failed upload abort others`() {
        val contentUploader = contentUploader()
        val fastPage = aPage("Fast page") {
            every { children } returns emptyList()
        }
        val failedPage = aPage("Failed page")
        val slowPage = aPage("Slow page")

        val fastServerPage = ServerPage("fastId", "Fast page", "id", emptyList(), emptyList())
        coEvery { uploadOperations.createOrUpdatePageContent(fastPage, "TEST", "id") } returns PageOperationResult.NotModified(fastPage, fastServerPage)

        coEvery { uploadOperations.createOrUpdatePageContent(failedPage, any(), any()) } coAnswers {
            delay(100)
            throw RuntimeException("Upload failed")
        }

        val slowServerPage = ServerPage("slowId", "Slow page", "id", emptyList(), emptyList())
        coEvery { uploadOperations.createOrUpdatePageContent(slowPage, "TEST", "id") } coAnswers {
            delay(20000)
            PageOperationResult.Created(slowPage, slowServerPage)
        }

        assertFailure {
            runBlocking { contentUploader.uploadPages(listOf(slowPage, failedPage, fastPage), "TEST", "id") }
        }.hasMessage("Upload failed")

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

        val defaultServerPage = ServerPage("defaultId", "Default page", "id", emptyList(), emptyList())
        coEvery { uploadOperations.createOrUpdatePageContent(defaultPage, "TEST", "id") } returns PageOperationResult.Created(defaultPage, defaultServerPage)

        val parentIdServerPage = ServerPage("pIdPage", "Parent id", "123", emptyList(), emptyList())
        coEvery {
            uploadOperations.createOrUpdatePageContent(
                pageWithParentId,
                any(),
                "123"
            )
        } returns PageOperationResult.Created(pageWithParentId, parentIdServerPage)

        val parentTitleServerPage = ServerPage("tIdPage", "Parent title", "345", emptyList(), emptyList())
        coEvery {
            uploadOperations.createOrUpdatePageContent(
                pageWithParentTitle,
                "TEST",
                "345"
            )
        } returns PageOperationResult.Created(pageWithParentTitle, parentTitleServerPage)

        coEvery { confluenceClient.getPage("TEST", "Custom") } returns mockk { every { id } returns "345" }


        runBlocking {
            contentUploader.uploadPages(
                listOf(defaultPage, pageWithParentId, pageWithParentTitle),
                "TEST",
                "id"
            )
        }

        coVerify { uploadOperations.createOrUpdatePageContent(defaultPage, "TEST", "id") }
        coVerify { uploadOperations.createOrUpdatePageContent(pageWithParentId, "TEST", "123") }
        coVerify { uploadOperations.createOrUpdatePageContent(pageWithParentTitle, "TEST", "345") }
    }

    @Test
    internal fun `Removal of all orphans`() {
        val contentUploader = contentUploader(Cleanup.All)
        val pages = inputPagesStructure()
        givenPagesCreated(pages)
        givenChildPagesFound(serverPagesStructure(), "root_id")
        coEvery { uploadOperations.deletePageWithChildren(any()) } returns listOf()

        runBlocking { contentUploader.uploadPages(pages, "TEST", "root_id") }

        listOf("c", "d", "e", "a3", "a4", "a5", "a6", "b3", "b4", "a11", "a12", "b21").forEach { id ->
            coVerify { uploadOperations.deletePageWithChildren(withArg { assertThat(it.id).isEqualTo("id_$id") }) }
        }
        listOf("root_id", "a_root", "b_root", "a", "b", "a1", "a2", "b1", "b2").forEach {id ->
            coVerify(exactly = 0) { uploadOperations.deletePageWithChildren(withArg { assertThat(it.id).isEqualTo("id_$id") }) }
        }

    }

    @Test
    internal fun `Removal of managed pages`() {
        val contentUploader = contentUploader(Cleanup.Managed)
        val pages = inputPagesStructure()
        givenPagesCreated(pages)
        givenChildPagesFound(serverPagesStructure(), "root_id")
        coEvery { uploadOperations.deletePageWithChildren(any()) } returns listOf()

        runBlocking { contentUploader.uploadPages(pages, "TEST", "root_id") }

        listOf("d", "a3", "a5", "b4", "a11", "b21").forEach {id ->
            coVerify { uploadOperations.deletePageWithChildren(withArg { assertThat(it.id).isEqualTo("id_$id") }) }
        }
        listOf("root_id", "a_root", "b_root", "a", "b", "e", "a1", "a2", "a4", "a6", "b1", "b2", "b3", "a12").forEach { id ->
            coVerify(exactly = 0) { uploadOperations.deletePageWithChildren(withArg { assertThat(it.id).isEqualTo("id_$id") }) }
        }
    }

    @Test
    internal fun `Removal of no pages`() {
        val contentUploader = contentUploader(Cleanup.None)
        val pages = inputPagesStructure()
        givenPagesCreated(pages)
        givenChildPagesFound(serverPagesStructure(), "root_id")

        runBlocking { contentUploader.uploadPages(pages, "TEST", "root_id") }

        coVerify(exactly = 0) { uploadOperations.deletePageWithChildren(any()) }
    }

    @Test
    internal fun uploadOfVirtualPages() {
        val pages = listOf(
            page("a") {
                virtual = true
                children(
                    page("a1")
                )
            }
        )

        coEvery {
            uploadOperations.checkPageAndUpdateParentIfRequired("a", "TEST", "root")
        } returns ServerPage("id_a", "a", "", emptyList(), emptyList())
        givenPagesCreated(pages[0].children)

        val contentUploader = contentUploader()

        runBlocking { contentUploader.uploadPages(pages, "TEST", "root") }

        coVerify { uploadOperations.checkPageAndUpdateParentIfRequired("a", "TEST", "root") }

    }

    private fun inputPagesStructure(): List<Page> {
        return listOf(
            page("a") {
                parentId = "id_a_root"
                children(
                    page("a1"),
                    page("a2"),
                )
            },
            page("b") {
                parentId = "id_b_root"
                children(
                    page("b1") { parentId = "id_a" /*case when page is set to another parent*/ },
                    page("b2")
                )
            }
        )
    }

    private fun serverPagesStructure(): List<ServerPageNode> {
        return listOf(
            ServerPageNode(
                "a_root", managed = false, listOf(
                    ServerPageNode(
                        "a", managed = true, children = listOf(
                            ServerPageNode(
                                "a1",
                                managed = true,
                                children = listOf(ServerPageNode("a11"), ServerPageNode("a12", managed = false))
                            ),
                            ServerPageNode("a2", managed = true),
                            ServerPageNode("a3", managed = true),
                            ServerPageNode("a4", managed = false),
                            ServerPageNode("a5", managed = true),
                            ServerPageNode("a6", managed = true, tenant = "other"), /*other tenant*/
                            ServerPageNode("b1", managed = false),
                        )
                    ),
                    ServerPageNode(
                        "c", managed = false, children = listOf(
                            ServerPageNode("c1", managed = false),
                            ServerPageNode("c2", managed = true)
                        )
                    ),
                    ServerPageNode(
                        "d", managed = true, children = listOf(
                            ServerPageNode("d1")
                        )
                    ),
                    ServerPageNode("e", managed = true, tenant = "one more"), /*other tenant*/
                )
            ),
            ServerPageNode(
                "b_root", managed = false, listOf(
                    ServerPageNode(
                        "b", children = listOf(
                            ServerPageNode("b2", children = listOf(ServerPageNode("b21"))),
                            ServerPageNode("b3", managed = false),
                            ServerPageNode("b4", managed = true),
                        )
                    ),
                )
            )
        )
    }

    private fun page(title: String, configurer: PageBuilder.() -> Unit = { }): Page {
        val builder = PageBuilder()
        builder.title = title
        builder.configurer()
        return builder.build()
    }

    @DslMarker
    annotation class InputPagesBuilder

    @InputPagesBuilder
    class PageBuilder {
        var title: String = ""
        var children: MutableList<Page> = mutableListOf()
        var parentId: String? = null
        var virtual: Boolean = false

        fun children(vararg pages: Page) {
            children += pages
        }

        fun build(): Page {
            val titleValue = title
            val childValue = children
            val virtualValue = virtual
            return mockk {
                every { title } returns titleValue
                every { children } returns childValue
                every { virtual } returns virtualValue
                every { source } returns Path(titleValue)
                if (parentId != null) {
                    every { content.header.attributes } returns mapOf("parentId" to parentId)
                } else {
                    every { content.header.attributes } returns emptyMap()
                }
            }
        }
    }

    private data class ServerPageNode(
        val title: String,
        val managed: Boolean = true,
        val children: List<ServerPageNode> = emptyList(),
        val tenant: String? = null
    ) {
        val id: String
            get() = "id_$title"
    }

    private fun givenPagesCreated(pages: List<Page>) {
        for (page in pages) {
            coEvery { uploadOperations.createOrUpdatePageContent(page, any(), any()) } returns PageOperationResult.Created(
                page,
                mockk {
                    every { id } returns "id_${page.title}"
                    every { title } returns page.title
                }
            )
            givenPagesCreated(page.children)
        }
    }

    private fun givenChildPagesFound(pages: List<ServerPageNode>, parentId: String) {
        coEvery { uploadOperations.findChildPages(parentId) } returns pages.map {
            val pageProperties = buildMap<String, PageProperty>{
                if (it.managed) {
                    put(HASH_PROPERTY, PageProperty("", HASH_PROPERTY, "abc", mockk()))
                }
                if (it.tenant != null) {
                    put(TENANT_PROPERTY, PageProperty("", TENANT_PROPERTY, it.tenant, mockk()))
                }
            }
            ConfluencePage(
                id = it.id,
                title = it.title,
                type = ContentType.page,
                status = "",
                metadata = PageMetadata(
                    labels = null,
                    properties = pageProperties
                ),
                body = null,
                version = null,
                children = null,
                ancestors = null,
            )
        }
        for (page in pages) {
            givenChildPagesFound(page.children, page.id)
        }
    }

    private fun aPage(
        creationTime: Long,
        creationTimeRegistry: MutableMap<Page, Long>,
        attributesValues: Map<String, Any?> = emptyMap(),
        virtualValue: Boolean = false,
        block: Page.() -> Unit = {}
    ): Page {
        val page = aPage("Page created for ${creationTime}ms", attributesValues, virtualValue, block)
        creationTimeRegistry[page] = creationTime
        return page
    }

    private fun aPage(
        pageTitle: String,
        attributesValues: Map<String, Any?> = emptyMap(),
        virtualValue: Boolean = false,
        block: Page.() -> Unit = {}
    ): Page {
        val page = mockk<Page>(block = {
            every { content } returns mockk {
                every { header } returns mockk {
                    every { attributes } returns attributesValues
                }
            }
            every { title } returns pageTitle
            every { virtual } returns virtualValue
            block()
        })
        return page
    }
}