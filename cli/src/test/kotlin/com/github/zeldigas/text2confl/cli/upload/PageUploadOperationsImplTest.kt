package com.github.zeldigas.text2confl.cli.upload

import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.confclient.PageAttachmentInput
import com.github.zeldigas.confclient.model.Attachment
import com.github.zeldigas.confclient.model.Label
import com.github.zeldigas.text2confl.cli.config.EditorVersion
import com.github.zeldigas.text2confl.convert.PageContent
import com.github.zeldigas.text2confl.convert.PageHeader
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.io.path.Path

private const val PAGE_ID = "id"

@ExtendWith(MockKExtension::class)
internal class PageUploadOperationsImplTest(
    @MockK private val client: ConfluenceClient
) {

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
                        serverLabel("three")
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
    internal fun `No update of page labels when nothing to change`() {
        val operations = uploadOperations()

        runBlocking {
            operations.updatePageLabels(
                serverPage(labels = listOf(serverLabel("one"), serverLabel("two"))),
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
        PAGE_ID, null, labels = labels, attachments = attachments
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
                    PAGE_ID, null,
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

    private fun uploadOperations(): PageUploadOperationsImpl {
        return PageUploadOperationsImpl(client, "message", true, ChangeDetector.HASH, EditorVersion.V2)
    }
}