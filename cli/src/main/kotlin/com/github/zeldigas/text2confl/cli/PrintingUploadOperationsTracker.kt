import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.zeldigas.confclient.model.ConfluencePage
import com.github.zeldigas.text2confl.convert.Page
import com.github.zeldigas.text2confl.core.upload.*
import io.ktor.http.*
import java.util.concurrent.atomic.AtomicLong

class PrintingUploadOperationsTracker(
    val server: Url,
    val prefix: String = "",
    val printer: (msg: String) -> Unit = ::println
) : UploadOperationTracker {

    companion object {
        const val UILINK = "tinyui"
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
        ) return;

        updatedCount.incrementAndGet()

        when (pageResult) {
            is PageOperationResult.Created -> {
                printWithPrefix("${green("Created:")} ${pageInfo(pageResult.serverPage, pageResult.local)}")
            }

            is PageOperationResult.ContentModified,
            is PageOperationResult.LocationModified-> {
                describeModifiedPage("Updated:", pageResult.serverPage, pageResult.local, labelUpdate, attachmentsUpdated)
            }

            is PageOperationResult.NotModified -> {
                if (labelUpdate != LabelsUpdateResult.NotChanged || attachmentsUpdated != AttachmentsUpdateResult.NotChanged) {
                    describeModifiedPage("Updated labels/attachments:", pageResult.serverPage, pageResult.local, labelUpdate, attachmentsUpdated)
                }
            }
        }
    }

    private fun describeModifiedPage(
        operation: String,
        serverPage: ServerPage,
        local: Page,
        labelUpdate: LabelsUpdateResult,
        attachmentsUpdated: AttachmentsUpdateResult
    ) {
        val labelsAttachmentsInfo = labelsAttachmentsInfo(labelUpdate, attachmentsUpdated)
        printWithPrefix(
            "${cyan(operation)} ${
                pageInfo(
                    serverPage,
                    local
                )
            }${if (labelsAttachmentsInfo.isNotBlank()) " $labelsAttachmentsInfo" else "" }"
        )
    }

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

    private fun labelsAttachmentsInfo(labelsUpdateResult: LabelsUpdateResult, attachmentsUpdated: AttachmentsUpdateResult): String {
        val labelsInfo =  buildString {
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
            return ""
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
}