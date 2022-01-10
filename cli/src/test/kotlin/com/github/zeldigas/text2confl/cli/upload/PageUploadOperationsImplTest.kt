package com.github.zeldigas.text2confl.cli.upload

import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.confclient.model.Label
import com.github.zeldigas.text2confl.cli.config.EditorVersion
import com.github.zeldigas.text2confl.convert.PageContent
import com.github.zeldigas.text2confl.convert.PageHeader
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class PageUploadOperationsImplTest(
    @MockK private val client: ConfluenceClient
) {

    @Test
    internal fun `Update of page labels with deletion of missing`() {
        val operations = uploadOperations()

        coEvery { client.addLabels("id", any()) } just Runs
        coEvery { client.deleteLabel("id", "three") } just Runs

        runBlocking {
            operations.updatePageLabels(
                ServerPage(
                    "id", null,
                    labels = listOf(
                        serverLabel("one"),
                        serverLabel("two"),
                        serverLabel("three")
                    ), attachments = emptyList()
                ), PageContent(PageHeader("title", mapOf("labels" to listOf("one", "two", "four", "five"))), "body", emptyList())
            )
        }

        coVerify{ client.addLabels("id", listOf("four", "five")) }
        coVerify{ client.deleteLabel("id", "three") }
    }

    @Test
    internal fun `No update of page labels when nothing to change`() {
        val operations = uploadOperations()

        runBlocking {
            operations.updatePageLabels(
                ServerPage(
                    "id", null,
                    labels = listOf(
                        serverLabel("one"),
                        serverLabel("two")
                    ), attachments = emptyList()
                ), PageContent(PageHeader("title", mapOf("labels" to listOf("one", "two"))), "body", emptyList())
            )
        }

        coVerify(exactly = 0) { client.addLabels(any(), any()) }
        coVerify(exactly = 0) { client.deleteLabel(any(), any()) }
    }

    private fun serverLabel(label: String) = Label("", label, label, label)

    private fun uploadOperations(): PageUploadOperationsImpl {
        return PageUploadOperationsImpl(client, "message", true, ChangeDetector.HASH, EditorVersion.V2)
    }
}