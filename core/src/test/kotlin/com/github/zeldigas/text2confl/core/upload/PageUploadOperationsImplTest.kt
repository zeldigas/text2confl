package com.github.zeldigas.text2confl.core.upload

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.github.zeldigas.confclient.*
import com.github.zeldigas.confclient.model.*
import com.github.zeldigas.text2confl.convert.EditorVersion
import com.github.zeldigas.text2confl.convert.Page
import com.github.zeldigas.text2confl.convert.PageContent
import com.github.zeldigas.text2confl.convert.PageHeader
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.io.path.Path

private const val PAGE_ID = "id"

@ExtendWith(MockKExtension::class)
internal class PageUploadOperationsImplTest(
    @MockK private val client: ConfluenceClient
) {

    @ValueSource(strings = ["", "value"])
    @ParameterizedTest
    internal fun `Creation of new page`(tenant: String) {
        coEvery {
            client.getPageOrNull(
                "TEST", "Page title", loadOptions = setOf(
                    SimplePageLoadOptions.Labels,
                    SimplePageLoadOptions.Version,
                    SimplePageLoadOptions.Attachments,
                    SimplePageLoadOptions.ParentId,
                    PagePropertyLoad(HASH_PROPERTY),
                    PagePropertyLoad(EDITOR_PROPERTY),
                    PagePropertyLoad(TENANT_PROPERTY),
                )
            )
        } returns null

        coEvery {
            client.createPage(
                any(), any()
            )
        } returns mockk {
            every { id } returns "new_id"
            every { title } returns "Page title"
            every { pageProperty(any()) } returns null
            every { labels } returns null
            every { attachments } returns null
            every { links } returns emptyMap()
        }
        coEvery { client.createPageProperty(any(), any(), any()) } just Runs

        val localPage = mockk<Page> {
            every { title } returns "Page title"
            every { content } returns mockk {
                every { body } returns "body"
                every { hash } returns "body-hash"
            }
            every { properties } returns emptyMap()
        }
        val result = runBlocking {
            uploadOperations(
                "create-page",
                false,
                tenant = tenant.ifEmpty { null }).createOrUpdatePageContent(localPage, "TEST", "parentId")
        }

        assertThat(result).isEqualTo(
            PageOperationResult.Created(
                localPage,
                ServerPage("new_id", "Page title", "parentId", emptyList(), emptyList())
            )
        )

        coVerify {
            client.createPage(
                PageContentInput("parentId", "Page title", "body", "TEST"),
                PageUpdateOptions(false, "create-page")
            )
        }
        coVerify {
            client.createPageProperty("new_id", "contenthash", PagePropertyInput.newProperty("body-hash"))
        }
        coVerify {
            client.createPageProperty("new_id", "editor", PagePropertyInput.newProperty("v2"))
        }
        if (tenant.isEmpty()) {
            coVerify(exactly = 0) {
                client.createPageProperty("new_id", "t2ctenant", any())
            }
        } else {
            coVerify { client.createPageProperty("new_id", "t2ctenant", PagePropertyInput.newProperty(tenant)) }
        }
    }

    @ValueSource(strings = ["", "value"])
    @ParameterizedTest
    internal fun `Update of existing page`(tenant: String) {
        val serverPage = ConfluencePage(
            id = PAGE_ID,
            title = "Page Title",
            labels = listOf(Label("", "one", "one", "one")),
            properties = buildMap {
                put("editor", PageProperty("123", "editor", "v1", PropertyVersion(2)))
                put("contenthash", PageProperty("124", "contenthash", "abc", PropertyVersion(3)))
                if (tenant.isNotEmpty()) {
                    put("t2ctenant", PageProperty("125", "t2ctenant", tenant, PropertyVersion(1)))
                }
            },
            body = null,
            version = PageVersionInfo(42, false, null),
            attachments = mockk(),
            parentId = "parentId",
            space = null
        )
        coEvery {
            client.getPageOrNull(
                "TEST", "Page title", loadOptions = setOf(
                    SimplePageLoadOptions.Labels,
                    SimplePageLoadOptions.Version,
                    SimplePageLoadOptions.Attachments,
                    SimplePageLoadOptions.ParentId,
                    PagePropertyLoad(HASH_PROPERTY),
                    PagePropertyLoad(EDITOR_PROPERTY),
                    PagePropertyLoad(TENANT_PROPERTY),
                    PagePropertyLoad("extra"),
                )
            )
        } returns serverPage

        coEvery { client.renamePage(any(), "Page title", any()) } returns mockk {
            every { version?.number } returns 43
            every { title } returns "Page title"
        }
        coEvery { client.updatePage(PAGE_ID, any(), any()) } returns mockk()
        coEvery { client.createPageProperty(any(), any(), any()) } just Runs
        coEvery { client.updatePageProperty(any(), any(), any()) } just Runs
        coEvery { client.fetchAllAttachments(any()) } returns listOf(serverAttachment("one", "HASH:123"))

        val localPage = mockk<Page> {
            every { title } returns "Page title"
            every { content } returns mockk {
                every { body } returns "body"
                every { hash } returns "body-hash"
            }
            every { properties } returns mapOf("extra" to "value")
        }
        val result = runBlocking {
            uploadOperations(
                "update-page",
                editorVersion = EditorVersion.V1,
                tenant = tenant.ifEmpty { null }).createOrUpdatePageContent(localPage, "TEST", "parentId")
        }

        assertThat(result).isEqualTo(
            PageOperationResult.ContentModified(
                localPage, ServerPage(
                    PAGE_ID, "Page title", "parentId",
                    listOf(serverLabel("one")),
                    listOf(serverAttachment("one", "HASH:123"))
                ), parentChanged = false
            )
        )

        coVerify {
            client.updatePage(
                PAGE_ID,
                PageContentInput("parentId", "Page title", "body", null, 44),
                PageUpdateOptions(true, "update-page")
            )
        }
        coVerify {
            client.createPageProperty(PAGE_ID, "extra", PagePropertyInput.newProperty("value"))
        }
        coVerify {
            client.updatePageProperty(
                PAGE_ID,
                serverPage.properties!!.getValue("contenthash"),
                PagePropertyInput("body-hash", PropertyVersion(4))
            )
        }
        coVerify(exactly = 0) {
            client.updatePageProperty(PAGE_ID, serverPage.properties!!.getValue("editor"), any())
        }
    }

    @EnumSource(ChangeDetector::class)
    @ParameterizedTest
    internal fun `Only properties update if content is not changed`(changeDetector: ChangeDetector) {
        coEvery {
            client.getPageOrNull(
                "TEST", "Page title", loadOptions = setOf(
                    SimplePageLoadOptions.Labels,
                    SimplePageLoadOptions.Version,
                    SimplePageLoadOptions.Attachments,
                    SimplePageLoadOptions.ParentId,
                    PagePropertyLoad(HASH_PROPERTY),
                    PagePropertyLoad(EDITOR_PROPERTY),
                    PagePropertyLoad(TENANT_PROPERTY),
                    PagePropertyLoad("extra"),
                ) + changeDetector.extraOptions
            )
        } returns mockk {
            every { id } returns PAGE_ID
            every { title } returns "Page title"
            every { labels } returns emptyList()
            every { attachments } returns mockk()
            every { parentId } returns "parentId"
            every { pageProperty(EDITOR_PROPERTY) } returns PageProperty(
                "123",
                EDITOR_PROPERTY,
                "v2",
                PropertyVersion(1)
            )
            every { pageProperty(TENANT_PROPERTY) } returns null
            every { pageProperty("extra") } returns PageProperty(
                "124",
                "extra",
                "value",
                PropertyVersion(3)
            )
            every { pageProperty("contenthash") } returns PageProperty(
                "124",
                "contenthash",
                "hash",
                PropertyVersion(3)
            )
            when (changeDetector) {
                ChangeDetector.CONTENT -> {
                    every { body?.storage?.value } returns "body"
                }

                else -> {}
            }
            every { links } returns emptyMap()
        }
        coEvery { client.fetchAllAttachments(any()) } returns emptyList()
        coEvery { client.updatePageProperty(PAGE_ID, any(), any()) } just Runs

        val result = runBlocking {
            uploadOperations(changeDetector = changeDetector).createOrUpdatePageContent(mockk {
                every { title } returns "Page title"
                every { content } returns mockk {
                    every { body } returns "body"
                    every { hash } returns "hash"
                }
                every { properties } returns mapOf("extra" to "updatedValue")
            }, "TEST", "parentId")
        }

        assertThat(result).isInstanceOf<PageOperationResult.NotModified>()
        assertThat(result.serverPage.id).isEqualTo(PAGE_ID)
        coVerify(exactly = 0) { client.updatePage(any(), any(), any()) }
        coVerify { client.updatePageProperty(PAGE_ID, any(), PagePropertyInput("updatedValue", PropertyVersion(4))) }
    }

    @Test
    internal fun `Update of existing page with setting explicit tenant`() {
        coEvery {
            client.getPageOrNull(
                "TEST", "Page title", loadOptions = setOf(
                    SimplePageLoadOptions.Labels,
                    SimplePageLoadOptions.Version,
                    SimplePageLoadOptions.Attachments,
                    SimplePageLoadOptions.ParentId,
                    PagePropertyLoad(HASH_PROPERTY),
                    PagePropertyLoad(EDITOR_PROPERTY),
                    PagePropertyLoad(TENANT_PROPERTY),
                    PagePropertyLoad("extra"),
                )
            )
        } returns mockk {
            every { id } returns PAGE_ID
            every { title } returns "Page title"
            every { version?.number } returns 42
            every { labels } returns listOf(serverLabel("one"))
            every { pageProperty("editor") } returns PageProperty("123", "editor", "v1", PropertyVersion(2))
            every { pageProperty("contenthash") } returns PageProperty("124", "contenthash", "abc", PropertyVersion(3))
            every { pageProperty("t2ctenant") } returns null
            every { pageProperty("extra") } returns null
            every { attachments } returns mockk()
            every { parentId } returns "parentId"
            every { links } returns emptyMap()
        }

        coEvery { client.updatePage(PAGE_ID, any(), any()) } returns mockk()
        coEvery { client.createPageProperty(any(), any(), any()) } just Runs
        coEvery { client.updatePageProperty(any(), any(), any()) } just Runs
        coEvery { client.fetchAllAttachments(any()) } returns listOf(serverAttachment("one", "HASH:123"))

        val localPage = mockk<Page> {
            every { title } returns "Page title"
            every { content } returns mockk {
                every { body } returns "body"
                every { hash } returns "body-hash"
            }
            every { properties } returns mapOf("extra" to "value")
        }
        val result = runBlocking {
            uploadOperations(
                "update-page",
                editorVersion = EditorVersion.V1,
                tenant = "value"
            ).createOrUpdatePageContent(localPage, "TEST", "parentId")
        }

        assertThat(result).isEqualTo(
            PageOperationResult.ContentModified(
                localPage, ServerPage(
                    PAGE_ID, "Page title", "parentId",
                    listOf(serverLabel("one")),
                    listOf(serverAttachment("one", "HASH:123"))
                )
            )
        )

        coVerify {
            client.createPageProperty(PAGE_ID, TENANT_PROPERTY, PagePropertyInput.newProperty("value"))
        }
    }

    @ValueSource(strings = ["parentId", "anotherParent"])
    @ParameterizedTest
    fun `Only location change`(targetParent: String) {
        val serverPage = ConfluencePage(
            id = PAGE_ID,
            title = "Page Title",
            labels = emptyList(),
            properties = buildMap {
                put("editor", PageProperty("123", "editor", "v1", PropertyVersion(2)))
                put("contenthash", PageProperty("124", "contenthash", "body-hash", PropertyVersion(3)))
            },
            body = null,
            version = PageVersionInfo(42, false, null),
            parentId = "parentId",
            attachments = mockk(),
            space = null
        )
        coEvery {
            client.getPageOrNull("TEST", "Page title", loadOptions = any())
        } returns serverPage

        val parentChanged = serverPage.parentId != targetParent

        coEvery { client.renamePage(any(), "Page title", any()) } returns mockk {
            every { version?.number } returns 43
            every { title } returns "Page title"
        }
        if (parentChanged) {
            coEvery { client.changeParent(any(), any(), 44, targetParent, any()) } returns mockk()
        }
        coEvery { client.createPageProperty(any(), any(), any()) } just Runs
        coEvery { client.fetchAllAttachments(any()) } returns listOf(serverAttachment("one", "HASH:123"))

        val localPage = mockk<Page> {
            every { title } returns "Page title"
            every { content } returns mockk {
                every { body } returns "body"
                every { hash } returns "body-hash"
            }
            every { properties } returns mapOf("extra" to "value")
        }
        val result = runBlocking {
            uploadOperations(
                "update-page",
                editorVersion = EditorVersion.V1,
            ).createOrUpdatePageContent(localPage, "TEST", targetParent)
        }

        assertThat(result).isEqualTo(
            PageOperationResult.LocationModified(
                localPage, ServerPage(
                    PAGE_ID, "Page title", targetParent,
                    listOf(),
                    listOf(serverAttachment("one", "HASH:123"))
                ), previousParent = "parentId", previousTitle = "Page Title"
            )
        )

        coVerify(exactly = 0) { client.updatePage(any(), any(), any()) }
        coVerify { client.renamePage(serverPage, "Page title", any()) }
        if (parentChanged) {
            coVerify { client.changeParent(PAGE_ID, "Page title", 44, targetParent, any()) }
        }
    }

    @ValueSource(strings = ["", "value"])
    @ParameterizedTest
    internal fun `Update of existing page with different tenant not allowed`(tenant: String) {
        coEvery {
            client.getPageOrNull(
                "TEST", "Page title", loadOptions = setOf(
                    SimplePageLoadOptions.Labels,
                    SimplePageLoadOptions.Version,
                    SimplePageLoadOptions.Attachments,
                    SimplePageLoadOptions.ParentId,
                    PagePropertyLoad(HASH_PROPERTY),
                    PagePropertyLoad(EDITOR_PROPERTY),
                    PagePropertyLoad(TENANT_PROPERTY),
                    PagePropertyLoad("extra"),
                )
            )
        } returns mockk {
            every { id } returns PAGE_ID
            every { title } returns "Page title"
            every { version?.number } returns 42
            every { labels } returns listOf(serverLabel("one"))
            every { pageProperty("editor") } returns PageProperty("123", "editor", "v1", PropertyVersion(2))
            every { pageProperty("contenthash") } returns PageProperty("124", "contenthash", "abc", PropertyVersion(3))
            every { pageProperty("t2ctenant") } returns PageProperty("124", "t2ctenant", "other", PropertyVersion(1))
            every { pageProperty("extra") } returns null
            every { attachments?.results } returns listOf(serverAttachment("one", "HASH:123"))
        }

        assertFailure {
            runBlocking {
                uploadOperations(
                    "update-page",
                    editorVersion = EditorVersion.V1,
                    tenant = tenant.ifEmpty { null }).createOrUpdatePageContent(mockk {
                    every { title } returns "Page title"
                    every { content } returns mockk {
                        every { body } returns "body"
                        every { hash } returns "body-hash"
                    }
                    every { properties } returns mapOf("extra" to "value")
                }, "TEST", "parentId")
            }
        }.isInstanceOf<InvalidTenantException>()
            .hasMessage("Page Page title must be in tenant \"${tenant.ifEmpty { "(no tenant)" }}\" but actual is \"other\"")
    }

    @Test
    internal fun `Update of page labels with deletion of missing`() {
        val operations = uploadOperations()

        coEvery { client.addLabels(PAGE_ID, any()) } just Runs
        coEvery { client.deleteLabel(PAGE_ID, "three") } just Runs

        val result = runBlocking {
            operations.updatePageLabels(
                serverPage(
                    labels = listOf(
                        serverLabel("one"),
                        serverLabel("two"),
                        serverLabel(null, "three")
                    )
                ),
                PageContent(
                    pageHeader(mapOf("labels" to listOf("one", "two", "four", "five"))), "body", emptyList()
                )
            )
        }

        assertThat(result).isEqualTo(
            LabelsUpdateResult.Updated(
                added = listOf("four", "five"),
                removed = listOf("three")
            )
        )

        coVerify { client.addLabels(PAGE_ID, listOf("four", "five")) }
        coVerify { client.deleteLabel(PAGE_ID, "three") }
    }

    @Test
    internal fun `Add of page labels to page without labels`() {
        val operations = uploadOperations()

        coEvery { client.addLabels(PAGE_ID, any()) } just Runs

        val result = runBlocking {
            operations.updatePageLabels(
                serverPage(
                    labels = emptyList()
                ),
                PageContent(
                    pageHeader(mapOf("labels" to listOf("one", "two", "four", "five"))), "body", emptyList()
                )
            )
        }

        assertThat(result).isEqualTo(
            LabelsUpdateResult.Updated(
                added = listOf("one", "two", "four", "five"),
                removed = emptyList()
            )
        )

        coVerify { client.addLabels(PAGE_ID, listOf("one", "two", "four", "five")) }
    }

    @Test
    internal fun `No update of page labels when nothing to change`() {
        val operations = uploadOperations()

        val result = runBlocking {
            operations.updatePageLabels(
                serverPage(labels = listOf(serverLabel("one"), serverLabel(null, "two"))),
                PageContent(pageHeader(mapOf("labels" to listOf("one", "two"))), "body", emptyList())
            )
        }

        assertThat(result).isEqualTo(LabelsUpdateResult.NotChanged)

        coVerify(exactly = 0) { client.addLabels(any(), any()) }
        coVerify(exactly = 0) { client.deleteLabel(any(), any()) }
    }

    private fun serverPage(
        labels: List<Label> = emptyList(),
        attachments: List<Attachment> = emptyList()
    ) = ServerPage(
        PAGE_ID, "Title", "parent_id", labels = labels, attachments = attachments
    )

    private fun pageHeader(attributes: Map<String, List<String>> = emptyMap()) = PageHeader("title", attributes)

    @Test
    internal fun `Upload of attachments for page`() {
        val operations = uploadOperations()

        coEvery { client.deleteAttachment(any()) } just Runs
        coEvery { client.updateAttachment(any(), any(), any()) } returns mockk()
        coEvery { client.addAttachments(any(), any()) } returns mockk()

        val largeSize = 2L * (1024 * 1024) + 1L
        val localAttachments = listOf(
            pageAttachment("one", "aaa", "test.txt", 10L),
            pageAttachment("two", "1234", "test.jpg", 512311L),
            pageAttachment("three", "456", "test.docx", 1000L),
            pageAttachment("five", "ccc", "test.png", largeSize),
            pageAttachment("six", "ddd", "test.unknown", 10L)
        )
        val serverAttachments = listOf(
            serverAttachment("one", "unrelated"),
            serverAttachment("two", "a HASH:123 b"),
            serverAttachment("three", "HASH:456"),
            serverAttachment("four", "HASH:456"),
        )
        val result = runBlocking {
            operations.updatePageAttachments(
                serverPage = ServerPage(
                    PAGE_ID, "Title", "parent_id",
                    labels = emptyList(), attachments = serverAttachments
                ),
                content = PageContent(
                    pageHeader(), "body", localAttachments
                )
            )
        }

        assertThat(result).isEqualTo(
            AttachmentsUpdateResult.Updated(
                added = listOf(localAttachments[3], localAttachments[4]),
                modified = listOf(localAttachments[0], localAttachments[1]),
                removed = listOf(
                    serverAttachments[3]
                )
            )
        )

        coVerifySequence {
            client.addAttachments(
                PAGE_ID, listOf(
                    PageAttachmentInput("five", Path("test.png"),largeSize, "HASH:ccc", "image/png"),
                )
            )
            client.addAttachments(
                PAGE_ID, listOf(
                    PageAttachmentInput("six", Path("test.unknown"), 10L, "HASH:ddd", null)
                )
            )
            client.updateAttachment(
                PAGE_ID,
                "id_one",
                PageAttachmentInput("one", Path("test.txt"), 10L, "HASH:aaa", "text/plain")
            )
            client.updateAttachment(
                PAGE_ID,
                "id_two",
                PageAttachmentInput("two", Path("test.jpg"), 512311L, "HASH:1234", "image/jpeg")
            )
            client.deleteAttachment("id_four")
        }
    }

    private fun pageAttachment(
        name: String,
        attachmentHash: String,
        fileName: String,
        size:Long
    ): com.github.zeldigas.text2confl.convert.Attachment {
        return mockk {
            every { attachmentName } returns name
            every { linkName } returns name
            every { hash } returns attachmentHash
            every { resourceLocation } returns Path(fileName)
            every { fileSize } returns size
        }
    }

    private fun serverAttachment(name: String, comment: String?): Attachment = Attachment("id_$name", name, buildMap {
        if (comment != null) {
            put("comment", comment)
        }
    })

    @Test
    internal fun `No actions is taken if existing and pending attachments are empty`() {
        runBlocking {
            uploadOperations().updatePageAttachments(
                serverPage = serverPage(),
                content = PageContent(pageHeader(), "body", listOf())
            )
        }
        coVerify(exactly = 0) { client.deleteAttachment(any()) }
        coVerify(exactly = 0) { client.updateAttachment(any(), any(), any()) }
        coVerify(exactly = 0) { client.addAttachments(any(), any()) }
    }

    private fun serverLabel(label: String) = Label("", label, label, label)

    private fun serverLabel(label: String?, name: String) = Label("", name, name, label)

    private fun uploadOperations(
        changeMessage: String = "message",
        notifyWatchers: Boolean = true,
        editorVersion: EditorVersion = EditorVersion.V2,
        changeDetector: ChangeDetector = ChangeDetector.HASH,
        tenant: String? = null
    ): PageUploadOperationsImpl {
        return PageUploadOperationsImpl(client, changeMessage, notifyWatchers, changeDetector, editorVersion, tenant)
    }

    @Test
    internal fun `Search child pages`() {
        val expectedResult = listOf<ConfluencePage>(mockk())

        coEvery {
            client.findChildPages(
                "123",
                setOf(PagePropertyLoad(HASH_PROPERTY), PagePropertyLoad(TENANT_PROPERTY))
            )
        } returns expectedResult

        val result = runBlocking {
            uploadOperations().findChildPages("123")
        }

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    internal fun `Search delete page with children`() {
        val childSubpages = listOf<ConfluencePage>(mockk {
            every { id } returns "567"
        })
        val rootPage = mockk<ConfluencePage> {
            every { id } returns "123"
        }
        coEvery { client.findChildPages("123") } returns childSubpages
        coEvery { client.findChildPages("567") } returns emptyList()
        coEvery { client.deletePage(any()) } just Runs

        val result = runBlocking {
            uploadOperations().deletePageWithChildren(rootPage)
        }

        assertThat(result).isEqualTo(listOf(rootPage) + childSubpages)

        coVerifyOrder {
            client.findChildPages("123")
            client.findChildPages("567")
            client.deletePage("567")
            client.deletePage("123")
        }
    }

    @Test
    internal fun virtualPageWithWrongParent() {
        coEvery {
            client.getPageOrNull(any(), any(), loadOptions = any())
        } returns mockk {
            every { id } returns "page_id"
            every { title } returns "Title"
            every { parentId } returns "wrong_id"
            every { version } returns mockk {
                every { number } returns 1
            }
            every { labels } returns null
            every { attachments } returns null
            every { pageProperty(TENANT_PROPERTY) } returns null
            every { links } returns emptyMap()
        }

        coEvery {
            client.changeParent("page_id", "Title", 2, "id", any())
        } returns mockk {

        }

        val result = runBlocking {
            uploadOperations().checkPageAndUpdateParentIfRequired("Title", "TEST", "id")
        }

        coVerify { client.changeParent("page_id", "Title", 2, "id", any()) }

        assertThat(result).isEqualTo(ServerPage("page_id", "Title", "id", emptyList(), emptyList()))
    }

    @ValueSource(strings = ["", "tenant"])
    @ParameterizedTest
    internal fun `Virtual page with wrong parent cannot be changed if it has different tenant`(tenant: String) {
        coEvery {
            client.getPageOrNull(any(), any(), loadOptions = any())
        } returns mockk {
            every { id } returns "page_id"
            every { title } returns "Title"
            every { parentId } returns "wrong_id"
            every { version } returns mockk {
                every { number } returns 1
            }
            every { labels } returns null
            every { attachments } returns null
            every { pageProperty(TENANT_PROPERTY) } returns mockk { every { value } returns "another" }
        }

        coEvery {
            client.changeParent("page_id", "Title", 2, "id", any())
        } returns mockk {

        }

        assertFailure {
            runBlocking {
                uploadOperations(tenant = tenant.ifEmpty { null }).checkPageAndUpdateParentIfRequired(
                    "Title",
                    "TEST",
                    "id"
                )
            }
        }.isInstanceOf<InvalidTenantException>()
    }

    @Test
    internal fun virtualPageWithCorrectParent() {
        coEvery {
            client.getPageOrNull(any(), any(), loadOptions = any())
        } returns mockk {
            every { id } returns "page_id"
            every { title } returns "Title"
            every { parentId } returns "id"
            every { labels } returns null
            every { attachments } returns null
            every { links } returns emptyMap()
        }

        runBlocking {
            uploadOperations().checkPageAndUpdateParentIfRequired("Title", "TEST", "id")
        }

        coVerify(exactly = 0) { client.changeParent(any(), any(), any(), any(), any()) }
    }

    @Test
    internal fun missingVirtualPageThrowsError() {
        coEvery {
            client.getPageOrNull(
                any(),
                any(),
                loadOptions = setOf(
                    SimplePageLoadOptions.ParentId, SimplePageLoadOptions.Version,
                    PagePropertyLoad(TENANT_PROPERTY)
                )
            )
        } returns null

        assertFailure {
            runBlocking { uploadOperations().checkPageAndUpdateParentIfRequired("page title", "TEST", "parentId") }
        }.isInstanceOf(PageNotFoundException::class)
            .isEqualTo(PageNotFoundException(space = "TEST", title = "page title"))
    }

    @Test
    fun `Cycle with parent is detected for found page`() {
        coEvery {
            client.getPageOrNull(any(), any(), loadOptions = any())
        } returns mockk {
            every { id } returns "parentId"
            every { title } returns "Page title"
        }

        assertFailure {
            runBlocking {
                uploadOperations().createOrUpdatePageContent(mockk() {
                    every { title } returns "Page title"
                    every { properties } returns emptyMap()
                }, "TEST", "parentId")
            }
        }.isInstanceOf(PageCycleException::class)
            .isEqualTo(PageCycleException("parentId", "Page title"))
    }
}