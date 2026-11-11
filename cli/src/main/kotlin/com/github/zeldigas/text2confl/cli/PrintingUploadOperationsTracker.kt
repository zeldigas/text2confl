import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.zeldigas.confclient.model.ConfluencePage
import com.github.zeldigas.text2confl.convert.Page
import com.github.zeldigas.text2confl.core.upload.*
import com.github.zeldigas.text2confl.core.upload.comments.CommentOutcome
import com.github.zeldigas.text2confl.core.upload.comments.CommentStatus
import io.ktor.http.*
import java.util.concurrent.atomic.AtomicLong

class PrintingUploadOperationsTracker(
    val server: Url,
    val prefix: String = "",
    val printer: (msg: String) -> Unit = ::println
) : UploadOperationTracker {

    companion object {
        const val UILINK = "tinyui"
        private const val MAX_COMMENT_TEXT = 40
        private const val REF_LENGTH = 8
    }

    private val updatedCount = AtomicLong(0L)


    override fun pageUpdated(
        pageResult: PageOperationResult,
        labelUpdate: LabelsUpdateResult,
        attachmentsUpdated: AttachmentsUpdateResult
    ) {
        if (pageResult is PageOperationResult.NotModified
            && labelUpdate == LabelsUpdateResult.NotChanged
            && attachmentsUpdated == AttachmentsUpdateResult.NotChanged
        ) return

        updatedCount.incrementAndGet()

        when (pageResult) {
            is PageOperationResult.Created -> {
                printWithPrefix("${green("Created:")} ${pageInfo(pageResult.serverPage, pageResult.local)}")
            }

            is PageOperationResult.ContentModified -> {
                describeModifiedPage(
                    "Updated:", pageResult.serverPage, pageResult.local, labelUpdate, attachmentsUpdated,
                    pageResult.comments
                )
            }

            is PageOperationResult.LocationModified -> {
                describeModifiedPage(
                    "Updated:",
                    pageResult.serverPage,
                    pageResult.local,
                    labelUpdate,
                    attachmentsUpdated
                )
            }

            is PageOperationResult.NotModified -> {
                if (labelUpdate != LabelsUpdateResult.NotChanged || attachmentsUpdated != AttachmentsUpdateResult.NotChanged) {
                    describeModifiedPage(
                        "Updated labels/attachments:",
                        pageResult.serverPage,
                        pageResult.local,
                        labelUpdate,
                        attachmentsUpdated
                    )
                }
            }
        }
    }

    private fun describeModifiedPage(
        operation: String,
        serverPage: ServerPage,
        local: Page,
        labelUpdate: LabelsUpdateResult,
        attachmentsUpdated: AttachmentsUpdateResult,
        comments: CommentRestoration? = null
    ) {
        val labelsAttachmentsInfo = labelsAttachmentsInfo(labelUpdate, attachmentsUpdated)
        val commentsInfo = when (comments) {
            is CommentRestoration.Failed -> red(
                if (comments.dry) "Inline comments (dry run): restoration failed, see log."
                else "Inline comments: restoration failed, see log."
            )

            null, is CommentRestoration.Dropped -> ""
            is CommentRestoration.Applied -> comments.takeIf { it.comments.isNotEmpty() }
                ?.let { commentsSummary(it) } ?: ""
        }
        val summary = "${cyan(operation)} ${
            pageInfo(
                serverPage,
                local
            )
        }${if (labelsAttachmentsInfo.isNotBlank()) " $labelsAttachmentsInfo" else ""}${
            if (commentsInfo.isNotBlank()) " $commentsInfo" else ""
        }"
        val details = (comments as? CommentRestoration.Applied)?.let { commentDetails(it) } ?: emptyList()
        // single print call, so that lines of concurrently uploaded pages don't intermix
        printWithPrefix(listOf(summary) + details)
    }

    private fun commentsSummary(restoration: CommentRestoration.Applied): String {
        val byStatus = restoration.comments.groupingBy { it.status }.eachCount()
        val kept = byStatus[CommentStatus.KEPT] ?: 0
        val adjusted = byStatus[CommentStatus.ADJUSTED] ?: 0
        val lost = (byStatus[CommentStatus.LOST] ?: 0) + (byStatus[CommentStatus.PARTIAL] ?: 0)
        val parts = if (restoration.dry) {
            listOfNotNull(
                green("would keep $kept"),
                if (adjusted > 0) yellow("adjust $adjusted") else null,
                if (lost > 0) red("lose $lost") else null,
            )
        } else {
            listOfNotNull(
                green("$kept kept"),
                if (adjusted > 0) yellow("$adjusted adjusted") else null,
                if (lost > 0) red("$lost lost") else null,
            )
        }
        val title = if (restoration.dry) "Inline comments (dry run, removed from page):" else "Inline comments:"
        return parts.joinToString(", ", prefix = "$title ", postfix = ".")
    }

    private fun commentDetails(restoration: CommentRestoration.Applied): List<String> {
        val dry = restoration.dry
        return restoration.comments.mapNotNull { comment ->
            when (comment.status) {
                CommentStatus.LOST, CommentStatus.PARTIAL -> {
                    val label = when {
                        dry && comment.status == CommentStatus.PARTIAL -> "Would partially lose:"
                        dry -> "Would lose:"
                        comment.status == CommentStatus.PARTIAL -> "Partially lost comment:"
                        else -> "Lost comment:"
                    }
                    val reason = comment.reason?.let { " - ${it.name.lowercase().replace('_', ' ')}" } ?: ""
                    "    ${red(label)} \"${shorten(comment.oldText)}\" (ref ${shortRef(comment)})$reason"
                }

                CommentStatus.ADJUSTED -> {
                    val label = if (dry) "Would adjust:" else "Adjusted comment:"
                    "    ${yellow(label)} \"${shorten(comment.oldText)}\" -> \"${shorten(comment.newText ?: "")}\" (ref ${
                        shortRef(
                            comment
                        )
                    })"
                }

                CommentStatus.KEPT -> null
            }
        }
    }

    private fun shorten(text: String): String =
        if (text.length <= MAX_COMMENT_TEXT) text else text.take(MAX_COMMENT_TEXT - 1) + "…"

    private fun shortRef(comment: CommentOutcome): String =
        if (comment.ref.length <= REF_LENGTH) comment.ref else comment.ref.take(REF_LENGTH) + "…"

    private fun pageInfo(serverPage: ServerPage, page: Page): String = buildString {
        append('"')
        append(blue(serverPage.title))
        append('"')
        append(" from - ")
        append(page.source.normalize())
        append(".")
        val uiLink = serverPage.links[UILINK]
        if (uiLink != null) {
            append(" URL - ")
            append(URLBuilder(server).appendPathSegments(uiLink).buildString())
            append(".")
        }
    }

    private fun labelsAttachmentsInfo(
        labelsUpdateResult: LabelsUpdateResult,
        attachmentsUpdated: AttachmentsUpdateResult
    ): String {
        val labelsInfo = buildString {
            if (labelsUpdateResult is LabelsUpdateResult.Updated) {
                append("Labels ")
                if (labelsUpdateResult.added.isNotEmpty()) {
                    append(green("+"))
                    append("[")
                    append(labelsUpdateResult.added.joinToString(", "))
                    append("]")
                    if (labelsUpdateResult.removed.isNotEmpty()) {
                        append(", ")
                    }
                }
                if (labelsUpdateResult.removed.isNotEmpty()) {
                    append(red("-"))
                    append("[")
                    append(labelsUpdateResult.removed.joinToString(", "))
                    append("]")
                }
            }
        }
        val attachmentsInfo = buildString {
            if (attachmentsUpdated is AttachmentsUpdateResult.Updated) {
                append("attachments: ")
                append(green("added ${attachmentsUpdated.added.size}, "))
                append(cyan("modified ${attachmentsUpdated.modified.size}, "))
                append(red("removed ${attachmentsUpdated.removed.size}"))
            }
        }
        val labelsAttachmentsDetails = listOf(labelsInfo, attachmentsInfo).filter { it.isNotBlank() }
        return if (labelsAttachmentsDetails.isEmpty()) {
            ""
        } else {
            labelsAttachmentsDetails.joinToString(", ", postfix = ".")
        }
    }

    override fun uploadsCompleted() {
        val updated = updatedCount.get()
        if (updated == 0L) {
            printer(green("All pages are up to date"))
        }
    }

    override fun pagesDeleted(root: ConfluencePage, allDeletedPages: List<ConfluencePage>) {
        if (allDeletedPages.isEmpty()) return

        printWithPrefix(buildString {
            append(red("Deleted:"))
            append(" ")
            append(deletedPage(allDeletedPages[0]))
            if (allDeletedPages.size > 1) {
                append(" with subpages:")
            }
        })

        val tail = allDeletedPages.drop(1)
        if (tail.isNotEmpty()) {
            tail.forEach { page ->
                printWithPrefix("${red("  Deleted:")} ${deletedPage(page)}")
            }
        }
    }

    private fun deletedPage(confluencePage: ConfluencePage): String {
        return buildString {
            append("\"")
            append(blue(confluencePage.title))
            append("\"")
            append(" (")
            append(confluencePage.id)
            append(")")
        }
    }

    private fun printWithPrefix(msg: String) {
        printer("$prefix$msg")
    }

    private fun printWithPrefix(lines: List<String>) {
        printer(lines.joinToString("\n") { "$prefix$it" })
    }
}