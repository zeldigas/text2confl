package com.github.zeldigas.text2confl.core.upload

import com.github.zeldigas.confclient.model.ConfluencePage
import com.github.zeldigas.text2confl.convert.Page
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import java.util.concurrent.atomic.AtomicLong

class LoggingUploadOperationsTracker(
    val server: Url
) : UploadOperationTracker {

    companion object {
        const val UILINK = "tinyui"
        private val logger = KotlinLogging.logger { }
    }

    private val updatedCount = AtomicLong(0L)

    private fun print(msg: String) {
        logger.info { msg }
    }

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
                print("Created: ${pageInfo(pageResult.serverPage, pageResult.local)}")
            }

            is PageOperationResult.ContentModified-> {
                describeModifiedPage(
                    "Updated:",
                    pageResult.serverPage,
                    pageResult.local,
                    labelUpdate,
                    attachmentsUpdated
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
            is PageOperationResult.Failed -> {
                logger.error {
                    "Failed: ${failedPage(pageResult)}"
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
        print(
            "$operation ${
                pageInfo(
                    serverPage,
                    local
                )
            }${if (labelsAttachmentsInfo.isNotBlank()) " $labelsAttachmentsInfo" else "" }"
        )
    }

    private fun pageInfo(serverPage: ServerPage, page: Page): String = buildString {
        append('"')
        append(serverPage.title)
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
                    append("+")
                    append("[")
                    append(labelsUpdateResult.added.joinToString(", "))
                    append("]")
                    if (labelsUpdateResult.removed.isNotEmpty()) {
                        append(", ")
                    }
                }
                if (labelsUpdateResult.removed.isNotEmpty()) {
                    append("-")
                    append("[")
                    append(labelsUpdateResult.removed.joinToString(", "))
                    append("]")
                }
            }
        }
        val attachmentsInfo = buildString {
            if (attachmentsUpdated is AttachmentsUpdateResult.Updated) {
                append("attachments: ")
                append("added ${attachmentsUpdated.added.size}, ")
                append(("modified ${attachmentsUpdated.modified.size}, "))
                append("removed ${attachmentsUpdated.removed.size}")
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
            print("All pages are up to date")
        }
    }

    override fun pagesDeleted(root: ConfluencePage, allDeletedPages: List<ConfluencePage>) {
        if (allDeletedPages.isEmpty()) return

        print(buildString {
            append("Deleted: ")
            append(deletedPage(allDeletedPages[0]))
            if (allDeletedPages.size > 1) {
                append(" with subpages:")
            }
        })

        val tail = allDeletedPages.drop(1)
        if (tail.isNotEmpty()) {
            tail.forEach { page ->
                print("  Deleted:  ${deletedPage(page)}")
            }
        }
    }

    private fun deletedPage(confluencePage: ConfluencePage): String {
        return buildString {
            append("\"")
            append(confluencePage.title)
            append("\"")
            append(" (")
            append(confluencePage.id)
            append(")")
        }
    }

    private fun failedPage(error: PageOperationResult.Failed): String {
        return buildString {
            append("\"")
            append(error.local.title)
            append("\"")
            append(" (")
            append(error.status)
            append(")")
            append(" ")
            append(error.body)
        }
    }

}