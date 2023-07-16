package com.github.zeldigas.text2confl.cli.upload

import assertk.all
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.confclient.PageAttachmentInput
import com.github.zeldigas.confclient.PageContentInput
import com.github.zeldigas.confclient.PageUpdateOptions
import com.github.zeldigas.confclient.model.Attachment
import com.github.zeldigas.confclient.model.ConfluencePage
import com.github.zeldigas.confclient.model.PageAttachments
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.io.path.Path

@ExtendWith(MockKExtension::class)
internal class DryRunClientTest(
    @MockK val confluenceClient: ConfluenceClient
) {
    val dryRunClient = DryRunClient(confluenceClient)

    @Test
    internal fun `Page create is mocked`() {
        val result = runBlocking {
            dryRunClient.createPage(
                PageContentInput("abc", "A Title", "some content", "ABC"),
                PageUpdateOptions(true, "some message")
            )
        }

        coVerify(exactly = 0) { confluenceClient.createPage(any(), any()) }
        assertThat(result).all {
            prop(ConfluencePage::id).isEqualTo("(known after apply)")
            prop(ConfluencePage::title).isEqualTo("A Title")
        }
    }

    @Test
    internal fun `Page update is mocked`() {
        val result = runBlocking {
            dryRunClient.updatePage(
                "1234",
                PageContentInput("abc", "A Title", "some content", "ABC"),
                PageUpdateOptions(true, "some message")
            )
        }

        coVerify(exactly = 0) { confluenceClient.updatePage(any(), any(), any()) }
        assertThat(result).all {
            prop(ConfluencePage::id).isEqualTo("1234")
            prop(ConfluencePage::title).isEqualTo("A Title")
        }
    }

    @Test
    internal fun `Page delete is mocked`() {
        runBlocking {
            dryRunClient.deletePage("1234")
        }

        coVerify(exactly = 0) { confluenceClient.deletePage(any()) }
    }

    @Test
    internal fun `Lookup of child pages is mocked for new pages`() {
        val expectedChildPages = listOf<ConfluencePage>(mockk())
        coEvery { confluenceClient.findChildPages("123", any()) } returns expectedChildPages

        val resultOfExisting = runBlocking { dryRunClient.findChildPages("123", listOf("exp1")) }

        coVerify(exactly = 1) {confluenceClient.findChildPages("123", listOf("exp1"))}
        assertThat(resultOfExisting).isEqualTo(expectedChildPages)

        val resultOfNew = runBlocking { dryRunClient.findChildPages("(known after apply)", listOf("exp2")) }
        assertThat(resultOfNew).isEmpty()
        coVerify(exactly = 0) {confluenceClient.findChildPages("(known after apply)", listOf("exp2"))}

    }

    @Test
    internal fun `Page labels operations are mocked`() {
        runBlocking { dryRunClient.addLabels("123", listOf("one", "two")) }
        coVerify(exactly = 0) { confluenceClient.addLabels(any(), any()) }

        runBlocking { dryRunClient.deleteLabel("123", "label") }
        coVerify(exactly = 0) { confluenceClient.deleteLabel(any(), any()) }
    }

    @Test
    internal fun `Page attachment operations are mocked`() {
        runBlocking { dryRunClient.deleteAttachment("123") }
        coVerify(exactly = 0) { confluenceClient.deleteAttachment(any()) }

        val result = runBlocking {
            dryRunClient.updateAttachment(
                "123",
                "111",
                PageAttachmentInput("test", Path("test.txt"), "comment", "text/plain")
            )
        }
        assertThat(result).isEqualTo(Attachment("111", "test", emptyMap()))
        coVerify(exactly = 0) { confluenceClient.updateAttachment(any(), any(), any()) }

        val addResult = runBlocking {
            dryRunClient.addAttachments(
                "123",
                listOf(
                    PageAttachmentInput("test", Path("test.txt"), "comment", "text/plain")
                )
            )
        }
        assertThat(addResult).isEqualTo(PageAttachments(results = listOf(Attachment("(known after apply)", "test", emptyMap()))))
        coVerify(exactly = 0) { confluenceClient.addAttachments(any(), any()) }
    }

}

