package com.github.zeldigas.text2confl.cli.upload

import com.github.zeldigas.confclient.*
import com.github.zeldigas.confclient.model.*
import mu.KotlinLogging
import java.time.ZonedDateTime

class DryRunClient(private val realClient: ConfluenceClient) : ConfluenceClient by realClient {

    companion object {
        private val log = KotlinLogging.logger{}
        private const val UNDEFINED_ID = "(known after apply)"
    }

    override suspend fun createPage(
        value: PageContentInput,
        updateParameters: PageUpdateOptions,
        expansions: List<String>?
    ): ConfluencePage {
        log.info { "(dryrun) Creating page under parent ${value.parentPage} with title ${value.title}" }
        return ConfluencePage(UNDEFINED_ID, ContentType.page, "created", value.title, null, null, PageVersionInfo(value.version, true, ZonedDateTime.now()), null)
    }

    override suspend fun updatePage(
        pageId: String,
        value: PageContentInput,
        updateParameters: PageUpdateOptions
    ): ConfluencePage {
        log.info { "(dryrun) Updating page $pageId with title ${value.title}" }
        return ConfluencePage(pageId, ContentType.page, "updated", value.title, null, null, PageVersionInfo(value.version, true, ZonedDateTime.now()), null)
    }

    override suspend fun deletePage(pageId: String) {
        log.info { "(dryrun) Deleting page $pageId" }
    }

    override suspend fun findChildPages(pageId: String, expansions: List<String>?): List<ConfluencePage> {
        return if (pageId == UNDEFINED_ID) {
            emptyList()
        } else {
            realClient.findChildPages(pageId, expansions)
        }
    }

    override suspend fun setPageProperty(pageId: String, name: String, value: PagePropertyInput) {
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
        return PageAttachments(pageAttachmentInput.map { toServerAttachment(UNDEFINED_ID, it) })
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