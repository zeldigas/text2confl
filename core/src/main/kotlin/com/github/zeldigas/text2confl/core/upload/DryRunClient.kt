package com.github.zeldigas.text2confl.core.upload

import com.github.zeldigas.confclient.*
import com.github.zeldigas.confclient.model.*
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.ZonedDateTime

class DryRunClient(private val realClient: ConfluenceClient) : ConfluenceClient by realClient {

    companion object {
        private val log = KotlinLogging.logger {}
        private const val UNDEFINED_ID = "(known after apply)"
    }

    override suspend fun createPage(
        value: PageContentInput,
        updateParameters: PageUpdateOptions,
        loadOptions: Set<PageLoadOptions>
    ): ConfluencePage {
        log.info { "(dryrun) Creating page under parent ${value.parentPage} with title ${value.title}" }
        return ConfluencePage(
            id = UNDEFINED_ID,
            title=value.title,
            version = PageVersionInfo(value.version, true, ZonedDateTime.now())
        )
    }

    override suspend fun updatePage(
        pageId: String,
        value: PageContentInput,
        updateParameters: PageUpdateOptions
    ): ConfluencePage {
        log.info { "(dryrun) Updating page $pageId with title ${value.title}" }
        return ConfluencePage(
            pageId,
            title = value.title,
            version = PageVersionInfo(value.version, true, ZonedDateTime.now())
        )
    }

    override suspend fun changeParent(
        pageId: String,
        title: String,
        version: Int,
        newParentId: String,
        updateParameters: PageUpdateOptions
    ): ConfluencePage {
        log.info { "(dryrun) Changing parent of page $pageId with title ${title} to $newParentId" }
        return ConfluencePage(
            id = pageId,
            title=title,
            version = PageVersionInfo(version, true, ZonedDateTime.now())
        )
    }

    override suspend fun renamePage(
        serverPage: ConfluencePage,
        newTitle: String,
        updateParameters: PageUpdateOptions
    ): ConfluencePage {
        log.info { "(dryrun) Changing title of page with ${serverPage.id}: ${serverPage.title} -> $newTitle" }
        return serverPage.copy(
            title = newTitle,
            version = PageVersionInfo(serverPage.version!!.number + 1, true, ZonedDateTime.now())
        )
    }

    override suspend fun deletePage(pageId: String) {
        log.info { "(dryrun) Deleting page $pageId" }
    }

    override suspend fun findChildPages(
        pageId: String,
        loadOptions: Set<PageLoadOptions>?
    ): List<ConfluencePage> {
        return if (pageId == UNDEFINED_ID) {
            emptyList()
        } else {
            realClient.findChildPages(pageId, loadOptions)
        }
    }

    override suspend fun createPageProperty(pageId: String, name: String, value: PagePropertyInput) {
        log.info { "(dryrun) Creating property on page $pageId: $name=${value.value}, version=${value.version.number}" }
    }

    override suspend fun updatePageProperty(
        pageId: String,
        property: PageProperty,
        value: PagePropertyInput
    ) {
        log.info { "(dryrun) Updating property on page $pageId: ${property.key}=${value.value}, version=${value.version.number}" }
    }

    override suspend fun deleteLabel(pageId: String, label: String) {
        log.info { "(dryrun) Deleting label on page $pageId: $label" }
    }

    override suspend fun addLabels(pageId: String, labels: List<String>) {
        log.info { "(dryrun) Adding labels on page $pageId: $labels" }
    }

    override suspend fun addAttachments(
        pageId: String,
        pageAttachmentInput: List<PageAttachmentInput>
    ): PageAttachments {
        pageAttachmentInput.forEach {
            log.info { "(dryrun) Creating attachment on page $pageId: ${contentDetails(it)}" }
        }
        return PageAttachments(results = pageAttachmentInput.map { toServerAttachment(UNDEFINED_ID, it) })
    }

    override suspend fun updateAttachment(
        pageId: String,
        attachmentId: String,
        pageAttachmentInput: PageAttachmentInput
    ): Attachment {
        log.info { "(dryrun) Updating attachment $attachmentId on page $pageId: ${contentDetails(pageAttachmentInput)}" }
        return toServerAttachment(attachmentId, pageAttachmentInput)
    }

    private fun toServerAttachment(
        attachmentId: String,
        pageAttachmentInput: PageAttachmentInput
    ) = Attachment(attachmentId, pageAttachmentInput.name, emptyMap())

    private fun contentDetails(pageAttachmentInput: PageAttachmentInput) =
        "uploading ${pageAttachmentInput.content} with contentType=${pageAttachmentInput.contentType}, comment=${pageAttachmentInput.comment}"

    override suspend fun deleteAttachment(attachmentId: String) {
        log.info { "(dryrun) Deleting attachment $attachmentId" }
    }
}