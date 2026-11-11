package com.github.zeldigas.text2confl.cli

import PrintingUploadOperationsTracker
import PrintingUploadOperationsTracker.Companion.UILINK
import assertk.assertThat
import assertk.assertions.*
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.zeldigas.confclient.model.ConfluencePage
import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.Page
import com.github.zeldigas.text2confl.core.upload.AttachmentsUpdateResult
import com.github.zeldigas.text2confl.core.upload.CommentRestoration
import com.github.zeldigas.text2confl.core.upload.LabelsUpdateResult
import com.github.zeldigas.text2confl.core.upload.PageOperationResult
import com.github.zeldigas.text2confl.core.upload.ServerPage
import com.github.zeldigas.text2confl.core.upload.comments.CommentOutcome
import com.github.zeldigas.text2confl.core.upload.comments.CommentStatus
import com.github.zeldigas.text2confl.core.upload.comments.LossReason
import io.ktor.http.*
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class PrintingUploadOperationsTrackerTest {
    private val messages = mutableListOf<String>()

    private val tracker = PrintingUploadOperationsTracker(Url("http://wiki.example.org/wiki"), "pref ", messages::add)

    @Test
    fun `Tracking of created page`() {
        tracker.pageUpdated(
            PageOperationResult.Created(local = local("a.md"), serverPage = server("Title", "/x/url")),
            LabelsUpdateResult.Updated(
                added = listOf("hello"), removed = listOf("world", "test")
            ),
            AttachmentsUpdateResult.Updated(
                added = listOf(attachment("a")),
                removed = listOf(serverAttachment("b"), serverAttachment("c"))
            )
        )

        assertThat(messages).isEqualTo(
            listOf(
                """pref ${green("Created:")} "${blue("Title")}" from - a.md. URL - http://wiki.example.org/wiki/x/url."""
            )
        )
    }

    @Test
    fun `Tracking of modified content`() {
        tracker.pageUpdated(
            PageOperationResult.ContentModified(
                local = local("a.md"),
                serverPage = server("Title", "/x/url"),
                comments = CommentRestoration.Dropped
            ),
            LabelsUpdateResult.NotChanged, AttachmentsUpdateResult.NotChanged
        )

        assertThat(messages).isEqualTo(
            listOf(
                """pref ${cyan("Updated:")} "${blue("Title")}" from - a.md. URL - http://wiki.example.org/wiki/x/url."""
            )
        )
    }

    @Test
    fun `No comments info when restoration found no comments`() {
        tracker.pageUpdated(
            contentModified(CommentRestoration.Applied(false, emptyList())),
            LabelsUpdateResult.NotChanged, AttachmentsUpdateResult.NotChanged
        )

        assertThat(messages).isEqualTo(
            listOf(
                """pref ${cyan("Updated:")} "${blue("Title")}" from - a.md. URL - http://wiki.example.org/wiki/x/url."""
            )
        )
    }

    @Test
    fun `Comments restoration details`() {
        tracker.pageUpdated(
            contentModified(CommentRestoration.Applied(false, sampleOutcomes)),
            LabelsUpdateResult.NotChanged, AttachmentsUpdateResult.NotChanged
        )

        assertThat(messages).isEqualTo(
            listOf(
                listOf(
                    """pref ${cyan("Updated:")} "${blue("Title")}" from - a.md. URL - http://wiki.example.org/wiki/x/url. Inline comments: ${green("1 kept")}, ${yellow("1 adjusted")}, ${red("2 lost")}.""",
                    """pref     ${yellow("Adjusted comment:")} "my old long text" -> "my long text" (ref 5b1e0000…)""",
                    """pref     ${red("Lost comment:")} "exponential backoff" (ref 87342740…) - text changed""",
                    """pref     ${red("Partially lost comment:")} "${"a".repeat(39)}…" (ref short) - block not found""",
                ).joinToString("\n")
            )
        )
    }

    @Test
    fun `Dry comments restoration details`() {
        tracker.pageUpdated(
            contentModified(CommentRestoration.Applied(true, sampleOutcomes)),
            LabelsUpdateResult.NotChanged, AttachmentsUpdateResult.NotChanged
        )

        assertThat(messages).isEqualTo(
            listOf(
                listOf(
                    """pref ${cyan("Updated:")} "${blue("Title")}" from - a.md. URL - http://wiki.example.org/wiki/x/url. Inline comments (dry run, removed from page): ${green("would keep 1")}, ${yellow("adjust 1")}, ${red("lose 2")}.""",
                    """pref     ${yellow("Would adjust:")} "my old long text" -> "my long text" (ref 5b1e0000…)""",
                    """pref     ${red("Would lose:")} "exponential backoff" (ref 87342740…) - text changed""",
                    """pref     ${red("Would partially lose:")} "${"a".repeat(39)}…" (ref short) - block not found""",
                ).joinToString("\n")
            )
        )
    }

    @Test
    fun `Only kept comments`() {
        tracker.pageUpdated(
            contentModified(CommentRestoration.Applied(false, sampleOutcomes.take(1))),
            LabelsUpdateResult.NotChanged, AttachmentsUpdateResult.NotChanged
        )

        assertThat(messages).isEqualTo(
            listOf(
                """pref ${cyan("Updated:")} "${blue("Title")}" from - a.md. URL - http://wiki.example.org/wiki/x/url. Inline comments: ${green("1 kept")}."""
            )
        )
    }

    @Test
    fun `Comments restoration failure`() {
        tracker.pageUpdated(
            contentModified(CommentRestoration.Failed(false, RuntimeException("boom"))),
            LabelsUpdateResult.NotChanged, AttachmentsUpdateResult.NotChanged
        )
        tracker.pageUpdated(
            contentModified(CommentRestoration.Failed(true, RuntimeException("boom"))),
            LabelsUpdateResult.NotChanged, AttachmentsUpdateResult.NotChanged
        )

        assertThat(messages).isEqualTo(
            listOf(
                """pref ${cyan("Updated:")} "${blue("Title")}" from - a.md. URL - http://wiki.example.org/wiki/x/url. ${red("Inline comments: restoration failed, see log.")}""",
                """pref ${cyan("Updated:")} "${blue("Title")}" from - a.md. URL - http://wiki.example.org/wiki/x/url. ${red("Inline comments (dry run): restoration failed, see log.")}""",
            )
        )
    }

    private val sampleOutcomes = listOf(
        CommentOutcome("aaaa-1", "kept text", "kept text", CommentStatus.KEPT),
        CommentOutcome("5b1e0000-1111", "my old long text", "my long text", CommentStatus.ADJUSTED),
        CommentOutcome("87342740-a564", "exponential backoff", null, CommentStatus.LOST, LossReason.TEXT_CHANGED),
        CommentOutcome("short", "a".repeat(50), "a", CommentStatus.PARTIAL, LossReason.BLOCK_NOT_FOUND),
    )

    private fun contentModified(comments: CommentRestoration) = PageOperationResult.ContentModified(
        local = local("a.md"),
        serverPage = server("Title", "/x/url"),
        comments = comments
    )

    @Test
    fun `No change to page and labels with attachments`() {
        tracker.pageUpdated(
            PageOperationResult.NotModified(local = local("a.md"), serverPage = server("Title")),
            LabelsUpdateResult.NotChanged, AttachmentsUpdateResult.NotChanged
        )

        assertThat(messages).isEmpty()
    }

    @Test
    fun `Tracking of modified labels`() {
        tracker.pageUpdated(
            PageOperationResult.NotModified(local = local("a.md"), serverPage = server("Title")),
            LabelsUpdateResult.Updated(
                added = listOf("hello"), removed = listOf("world", "test")
            ), AttachmentsUpdateResult.NotChanged
        )

        assertThat(messages).isEqualTo(
            listOf(
                """pref ${cyan("Updated labels/attachments:")} "${blue("Title")}" from - a.md. Labels ${green("+")}[hello], ${
                    red(
                        "-"
                    )
                }[world, test]."""
            )
        )
    }

    @Test
    fun `Tracking of attachments change`() {
        tracker.pageUpdated(
            PageOperationResult.NotModified(local = local("a.md"), serverPage = server("Title")),
            LabelsUpdateResult.NotChanged,
            AttachmentsUpdateResult.Updated(
                added = listOf(attachment("a")),
                removed = listOf(serverAttachment("b"), serverAttachment("c"))
            )
        )

        assertThat(messages).isEqualTo(
            listOf(
                """pref ${cyan("Updated labels/attachments:")} "${blue("Title")}" from - a.md. attachments: ${green("added 1, ")}${
                    cyan(
                        "modified 0, "
                    )
                }${red("removed 2")}."""
            )
        )
    }

    @Test
    fun `No summary message if change change`() {
        tracker.pageUpdated(
            PageOperationResult.Created(local = local("a.md"), serverPage = server("Title")),
            LabelsUpdateResult.NotChanged, AttachmentsUpdateResult.NotChanged
        )

        tracker.uploadsCompleted()

        assertThat(messages).hasSize(1)
        assertThat(messages).index(0).isNotEqualTo(green("All pages are up to date"))
    }

    @Test
    fun `No changes summary message`() {
        tracker.pageUpdated(
            PageOperationResult.NotModified(local = local("a.md"), serverPage = server("Title")),
            LabelsUpdateResult.NotChanged, AttachmentsUpdateResult.NotChanged
        )

        tracker.uploadsCompleted()

        assertThat(messages).hasSize(1)
        assertThat(messages).index(0).isEqualTo(green("All pages are up to date"))
    }

    private fun local(location: String): Page = mockk {
        every { source } returns Path(location)
    }

    private fun server(title: String, link: String? = null) = ServerPage(
        "id", title, "parent", emptyList(), emptyList(),
        if (link != null) mapOf(UILINK to link) else emptyMap()
    )

    private fun attachment(name: String): Attachment = mockk {
        every { attachmentName } returns name
    }

    private fun serverAttachment(expectedName: String): com.github.zeldigas.confclient.model.Attachment = mockk {
        every { title } returns expectedName
    }

    @Test
    fun `Page removal tracking`() {
        val pages = listOf(confluencePage("a"))
        tracker.pagesDeleted(pages[0], pages)

        assertThat(messages).isEqualTo(
            listOf(
                """pref ${red("Deleted:")} "${blue("a")}" (id_a)"""
            )
        )
    }

    @Test
    fun `Page removal tracking with subpages`() {
        val pages = listOf(confluencePage("a"), confluencePage("b"), confluencePage("c"))
        tracker.pagesDeleted(pages[0], pages)

        assertThat(messages).isEqualTo(
            listOf(
                """pref ${red("Deleted:")} "${blue("a")}" (id_a) with subpages:""",
                """pref ${red("  Deleted:")} "${blue("b")}" (id_b)""",
                """pref ${red("  Deleted:")} "${blue("c")}" (id_c)"""
            )
        )
    }

    private fun confluencePage(expectedTitle: String): ConfluencePage = mockk {
        every { title } returns expectedTitle
        every { id } returns "id_${expectedTitle}"
    }


}