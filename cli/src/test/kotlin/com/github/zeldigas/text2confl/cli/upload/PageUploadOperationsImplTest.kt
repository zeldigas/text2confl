package com.github.zeldigas.text2confl.cli.upload

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.github.zeldigas.confclient.*
import com.github.zeldigas.confclient.model.*
import com.github.zeldigas.text2confl.cli.config.EditorVersion
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
                "TEST", "Page title", expansions = setOf(
                    "metadata.labels",
                    "metadata.properties.contenthash",
                    "metadata.properties.editor",
                    "metadata.properties.t2ctenant",
                    "version",
                    "children.attachment",
                    "ancestors"
                )
            )
        } returns null

        coEvery {
            client.createPage(
                any(), any(), listOf(
                    "metadata.labels",
                    "metadata.properties.contenthash",
                    "metadata.properties.editor",
                    "version",
                    "children.attachment"
                )
            )
        } returns mockk {
            every { id } returns "new_id"
            every { title } returns "Page title"
            every { pageProperty(any()) } returns null
            every { metadata } returns null
            every { children } returns null
        }
        coEvery { client.setPageProperty(any(), any(), any()) } just Runs

        val result = runBlocking {
            uploadOperations("create-page", false, tenant = tenant.ifEmpty { null }).createOrUpdatePageContent(mockk {
                every { title } returns "Page title"
                every { content } returns mockk {
                    every { body } returns "body"
                    every { hash } returns "body-hash"
                }
                every { properties } returns emptyMap()
            }, "TEST", "parentId")
        }

        assertThat(result).isEqualTo(ServerPage("new_id", "Page title", "parentId", emptyList(), emptyList()))

        coVerify {
            client.createPage(
                PageContentInput("parentId", "Page title", "body", "TEST"),
                PageUpdateOptions(false, "create-page"),
                any()
            )
        }
        coVerify {
            client.setPageProperty("new_id", "contenthash", PagePropertyInput.newProperty("body-hash"))
        }
        coVerify {
            client.setPageProperty("new_id", "editor", PagePropertyInput.newProperty("v2"))
        }
        if (tenant.isEmpty()) {
            coVerify(exactly = 0) {
                client.setPageProperty("new_id", "t2ctenant", any())
            }
        } else {
            coVerify { client.setPageProperty("new_id", "t2ctenant", PagePropertyInput.newProperty(tenant)) }
        }
    }

    @ValueSource(strings = ["", "value"])
    @ParameterizedTest
    internal fun `Update of existing page`(tenant: String) {
        coEvery {
            client.getPageOrNull(
                "TEST", "Page title", expansions = setOf(
                    "metadata.labels",
                    "metadata.properties.contenthash",
                    "metadata.properties.editor",
                    "metadata.properties.t2ctenant",
                    "metadata.properties.extra",
                    "version",
                    "children.attachment",
                    "ancestors"
                )
            )
        } returns mockk {
            every { id } returns PAGE_ID
            every { title } returns "Page title"
            every { version?.number } returns 42
            every { metadata?.labels?.results } returns listOf(serverLabel("one"))
            every { pageProperty("editor") } returns PageProperty("123", "editor", "v1", PropertyVersion(2))
            every { pageProperty("contenthash") } returns PageProperty("124", "contenthash", "abc", PropertyVersion(3))
            every { pageProperty("t2ctenant") } returns (if (tenant.isEmpty()) null else PageProperty("124", "contenthash", tenant, PropertyVersion(3)))
            every { pageProperty("extra") } returns null
            every { children?.attachment?.results } returns listOf(serverAttachment("one", "HASH:123"))
        }

        coEvery { client.updatePage(PAGE_ID, any(), any()) } returns mockk()
        coEvery { client.setPageProperty(any(), any(), any()) } just Runs

        val result = runBlocking {
            uploadOperations("update-page", editorVersion = EditorVersion.V1, tenant = tenant.ifEmpty { null }).createOrUpdatePageContent(mockk {
                every { title } returns "Page title"
                every { content } returns mockk {
                    every { body } returns "body"
                    every { hash } returns "body-hash"
                }
                every { properties } returns mapOf( "extra" to "value" )
            }, "TEST", "parentId")
        }

        assertThat(result).isEqualTo(
            ServerPage(
                PAGE_ID, "Page title", "parentId",
                listOf(serverLabel("one")),
                listOf(serverAttachment("one", "HASH:123"))
            )
        )

        coVerify {
            client.updatePage(
                PAGE_ID,
                PageContentInput("parentId", "Page title", "body", null, 43),
                PageUpdateOptions(true, "update-page")
            )
        }
        coVerify {
            client.setPageProperty(PAGE_ID, "contenthash", PagePropertyInput("body-hash", PropertyVersion(4)))
        }
        coVerify {
            client.setPageProperty(PAGE_ID, "extra", PagePropertyInput.newProperty("value"))
        }
        coVerify(exactly = 0) {
            client.setPageProperty(PAGE_ID, "editor", any())
        }
    }

    @EnumSource(ChangeDetector::class)
    @ParameterizedTest
    internal fun `Only properties update if content is not changed`(changeDetector: ChangeDetector) {
        coEvery {
            client.getPageOrNull(
                "TEST", "Page title", expansions = setOf(
                    "metadata.labels",
                    "metadata.properties.contenthash",
                    "metadata.properties.editor",
                    "metadata.properties.t2ctenant",
                    "metadata.properties.extra",
                    "version",
                    "children.attachment",
                    "ancestors"
                ) + changeDetector.extraData
            )
        } returns mockk {
            every { id } returns PAGE_ID
            every { title } returns "Page title"
            every { metadata?.labels?.results } returns emptyList()
            every { children?.attachment?.results } returns emptyList()
            every { ancestors } returns listOf(mockk { every { id } returns "parentId" })
            every { pageProperty(EDITOR_PROPERTY) } returns PageProperty("123", EDITOR_PROPERTY, "v2", PropertyVersion(1))
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
        }
        coEvery { client.setPageProperty(PAGE_ID, "extra", any()) } just Runs

        val result = runBlocking {
            uploadOperations(changeDetector = changeDetector).createOrUpdatePageContent(mockk {
                every { title } returns "Page title"
                every { content } returns mockk {
                    every { body } returns "body"
                    every { hash } returns "hash"
                }
                every { properties } returns mapOf( "extra" to "updatedValue")
            }, "TEST", "parentId")
        }

        assertThat(result.id).isEqualTo(PAGE_ID)
        coVerify(exactly = 0) { client.updatePage(any(), any(), any()) }
        coVerify { client.setPageProperty(PAGE_ID, "extra", PagePropertyInput("updatedValue", PropertyVersion(4))) }
    }

    @Test
    internal fun `Update of existing page with setting explicit tenant`() {
        coEvery {
            client.getPageOrNull(
                "TEST", "Page title", expansions = setOf(
                    "metadata.labels",
                    "metadata.properties.contenthash",
                    "metadata.properties.editor",
                    "metadata.properties.t2ctenant",
                    "metadata.properties.extra",
                    "version",
                    "children.attachment",
                    "ancestors"
                )
            )
        } returns mockk {
            every { id } returns PAGE_ID
            every { title } returns "Page title"
            every { version?.number } returns 42
            every { metadata?.labels?.results } returns listOf(serverLabel("one"))
            every { pageProperty("editor") } returns PageProperty("123", "editor", "v1", PropertyVersion(2))
            every { pageProperty("contenthash") } returns PageProperty("124", "contenthash", "abc", PropertyVersion(3))
            every { pageProperty("t2ctenant") } returns null
            every { pageProperty("extra") } returns null
            every { children?.attachment?.results } returns listOf(serverAttachment("one", "HASH:123"))
        }

        coEvery { client.updatePage(PAGE_ID, any(), any()) } returns mockk()
        coEvery { client.setPageProperty(any(), any(), any()) } just Runs

        val result = runBlocking {
            uploadOperations("update-page", editorVersion = EditorVersion.V1, tenant = "value").createOrUpdatePageContent(mockk {
                every { title } returns "Page title"
                every { content } returns mockk {
                    every { body } returns "body"
                    every { hash } returns "body-hash"
                }
                every { properties } returns mapOf( "extra" to "value" )
            }, "TEST", "parentId")
        }

        assertThat(result).isEqualTo(
            ServerPage(
                PAGE_ID, "Page title", "parentId",
                listOf(serverLabel("one")),
                listOf(serverAttachment("one", "HASH:123"))
            )
        )

        coVerify {
            client.setPageProperty(PAGE_ID, TENANT_PROPERTY, PagePropertyInput.newProperty("value"))
        }
    }

    @ValueSource(strings = ["", "value"])
    @ParameterizedTest
    internal fun `Update of existing page with different tenant not allowed`(tenant: String) {
        coEvery {
            client.getPageOrNull(
                "TEST", "Page title", expansions = setOf(
                    "metadata.labels",
                    "metadata.properties.contenthash",
                    "metadata.properties.editor",
                    "metadata.properties.t2ctenant",
                    "metadata.properties.extra",
                    "version",
                    "children.attachment",
                    "ancestors"
                )
            )
        } returns mockk {
            every { id } returns PAGE_ID
            every { title } returns "Page title"
            every { version?.number } returns 42
            every { metadata?.labels?.results } returns listOf(serverLabel("one"))
            every { pageProperty("editor") } returns PageProperty("123", "editor", "v1", PropertyVersion(2))
            every { pageProperty("contenthash") } returns PageProperty("124", "contenthash", "abc", PropertyVersion(3))
            every { pageProperty("t2ctenant") } returns PageProperty("124", "t2ctenant", "other", PropertyVersion(1))
            every { pageProperty("extra") } returns null
            every { children?.attachment?.results } returns listOf(serverAttachment("one", "HASH:123"))
        }

        assertFailure {  runBlocking {
            uploadOperations("update-page", editorVersion = EditorVersion.V1, tenant = tenant.ifEmpty { null }).createOrUpdatePageContent(mockk {
                every { title } returns "Page title"
                every { content } returns mockk {
                    every { body } returns "body"
                    every { hash } returns "body-hash"
                }
                every { properties } returns mapOf( "extra" to "value" )
            }, "TEST", "parentId")
        } }.isInstanceOf<InvalidTenantException>()
            .hasMessage("Page Page title must be in tenant \"${tenant.ifEmpty { "(no tenant)" }}\" but actual is \"other\"")
    }

    @Test
    internal fun `Update of page labels with deletion of missing`() {
        val operations = uploadOperations()

        coEvery { client.addLabels(PAGE_ID, any()) } just Runs
        coEvery { client.deleteLabel(PAGE_ID, "three") } just Runs

        runBlocking {
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

        coVerify { client.addLabels(PAGE_ID, listOf("four", "five")) }
        coVerify { client.deleteLabel(PAGE_ID, "three") }
    }

    @Test
    internal fun `Add of page labels to page without labels`() {
        val operations = uploadOperations()

        coEvery { client.addLabels(PAGE_ID, any()) } just Runs

        runBlocking {
            operations.updatePageLabels(
                serverPage(
                    labels = emptyList()
                ),
                PageContent(
                    pageHeader(mapOf("labels" to listOf("one", "two", "four", "five"))), "body", emptyList()
                )
            )
        }

        coVerify { client.addLabels(PAGE_ID, listOf("one", "two", "four", "five")) }
    }

    @Test
    internal fun `No update of page labels when nothing to change`() {
        val operations = uploadOperations()

        runBlocking {
            operations.updatePageLabels(
                serverPage(labels = listOf(serverLabel("one"), serverLabel(null, "two"))),
                PageContent(pageHeader(mapOf("labels" to listOf("one", "two"))), "body", emptyList())
            )
        }

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

        runBlocking {
            operations.updatePageAttachments(
                serverPage = ServerPage(
                    PAGE_ID, "Title", "parent_id",
                    labels = emptyList(), attachments = listOf(
                        serverAttachment("one", "unrelated"),
                        serverAttachment("two", "a HASH:123 b"),
                        serverAttachment("three", "HASH:456"),
                        serverAttachment("four", "HASH:456"),
                    )
                ),
                content = PageContent(
                    pageHeader(), "body", listOf(
                        pageAttachment("one", "aaa", "test.txt"),
                        pageAttachment("two", "1234", "test.jpg"),
                        pageAttachment("three", "456", "test.docx"),
                        pageAttachment("five", "ccc", "test.png"),
                        pageAttachment("six", "ddd", "test.unknown")
                    )
                )
            )
        }

        coVerifyAll {
            client.deleteAttachment("id_four")
            client.updateAttachment(
                PAGE_ID,
                "id_one",
                PageAttachmentInput("one", Path("test.txt"), "HASH:aaa", "text/plain")
            )
            client.updateAttachment(
                PAGE_ID,
                "id_two",
                PageAttachmentInput("two", Path("test.jpg"), "HASH:1234", "image/jpeg")
            )
            client.addAttachments(
                PAGE_ID, listOf(
                    PageAttachmentInput("five", Path("test.png"), "HASH:ccc", "image/png"),
                    PageAttachmentInput("six", Path("test.unknown"), "HASH:ddd", null)
                )
            )
        }
    }

    private fun pageAttachment(
        name: String,
        attachmentHash: String,
        fileName: String
    ): com.github.zeldigas.text2confl.convert.Attachment {
        return mockk {
            every { attachmentName } returns name
            every { linkName } returns name
            every { hash } returns attachmentHash
            every { resourceLocation } returns Path(fileName)
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

        coEvery { client.findChildPages("123", listOf("metadata.properties.$HASH_PROPERTY", "metadata.properties.$TENANT_PROPERTY")) } returns expectedResult

        val result = runBlocking {
            uploadOperations().findChildPages("123")
        }

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    internal fun `Search delete page with children`() {
        val expectedResult = listOf<ConfluencePage>(mockk {
            every { id } returns "567"
        })

        coEvery { client.findChildPages("123") } returns expectedResult
        coEvery { client.findChildPages("567") } returns emptyList()
        coEvery { client.deletePage(any()) } just Runs

        runBlocking {
            uploadOperations().deletePageWithChildren("123")
        }

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
            client.getPageOrNull(any(), any(), expansions = any())
        } returns mockk {
            every { id } returns "page_id"
            every { title } returns "Title"
            every { ancestors } returns listOf( mockk{ every { id } returns "wrong_id"})
            every { version } returns mockk {
                every { number } returns 1
            }
            every { children } returns null
            every { metadata } returns null
            every { pageProperty(TENANT_PROPERTY) } returns null
        }

        coEvery { client.changeParent("page_id", "Title", 2, "id", any())
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
            client.getPageOrNull(any(), any(), expansions = any())
        } returns mockk {
            every { id } returns "page_id"
            every { title } returns "Title"
            every { ancestors } returns listOf( mockk{ every { id } returns "wrong_id"})
            every { version } returns mockk {
                every { number } returns 1
            }
            every { children } returns null
            every { metadata } returns null
            every { pageProperty(TENANT_PROPERTY) } returns mockk { every { value } returns "another" }
        }

        coEvery { client.changeParent("page_id", "Title", 2, "id", any())
        } returns mockk {

        }

        assertFailure {
            runBlocking {
                uploadOperations(tenant = tenant.ifEmpty { null }).checkPageAndUpdateParentIfRequired("Title", "TEST", "id")
            }
        }.isInstanceOf<InvalidTenantException>()
    }

    @Test
    internal fun virtualPageWithCorrectParent() {
        coEvery {
            client.getPageOrNull(any(), any(), expansions = any())
        } returns mockk {
            every { id } returns "page_id"
            every { title } returns "Title"
            every { ancestors } returns listOf( mockk{ every { id } returns "id"})
            every { metadata } returns null
            every { children } returns null
        }

        runBlocking {
            uploadOperations().checkPageAndUpdateParentIfRequired("Title", "TEST", "id")
        }

        coVerify(exactly = 0) { client.changeParent(any(), any(), any(), any(), any()) }
    }

    @Test
    internal fun missingVirtualPageThrowsError() {
        coEvery {
            client.getPageOrNull(any(), any(), expansions = setOf("ancestors", "version", "metadata.properties.${TENANT_PROPERTY}"))
        } returns null

        assertFailure {
            runBlocking { uploadOperations().checkPageAndUpdateParentIfRequired("page title", "TEST", "parentId") }
        }.isInstanceOf(IllegalStateException::class)
            .hasMessage("Page not found in TEST: page title")
    }
}