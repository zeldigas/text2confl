package com.github.zeldigas.text2confl.core.upload

import com.github.zeldigas.confclient.*
import com.github.zeldigas.confclient.model.*
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.ZonedDateTime

class DryRunClient(private val realClient: ConfluenceClient) : ConfluenceClient by realClient {

    companion object {
        private val logger = KotlinLogging.logger{}
        private const val UNDEFINED_ID = "(known after apply)"
    }

    override suspend fun createPage(
        value: PageContentInput,
        updateParameters: PageUpdateOptions,
        expansions: List<String>?
    ): ConfluencePage {
        logger.info { "(dryrun) Creating page under parent ${value.parentPage} with title ${value.title}" }
        return ConfluencePage(UNDEFINED_ID, ContentType.page, "created", value.title, null, null, PageVersionInfo(value.version, true, ZonedDateTime.now()), null, null)
    }

    override suspend fun updatePage(
        pageId: String,
        value: PageContentInput,
        updateParameters: PageUpdateOptions
    ): ConfluencePage {
        logger.info { "(dryrun) Updating page $pageId with title ${value.title}" }
        return ConfluencePage(pageId, ContentType.page, "updated", value.title, null, null, PageVersionInfo(value.version, true, ZonedDateTime.now()), null, null)
    }

    override suspend fun changeParent(
        pageId: String,
        title: String,
        version: Int,
        newParentId: String,
        updateParameters: PageUpdateOptions
    ): ConfluencePage {
        logger.info { "(dryrun) Changing parent of page $pageId with title ${title} to $newParentId" }
        return ConfluencePage(pageId, ContentType.page, "updated", title, null, null, PageVersionInfo(version, true, ZonedDateTime.now()), null, null)
    }

    override suspend fun deletePage(pageId: String) {
        logger.info { "(dryrun) Deleting page $pageId" }
    }

    override suspend fun findChildPages(pageId: String, expansions: List<String>?): List<ConfluencePage> {
        return if (pageId == UNDEFINED_ID) {
            emptyList()
        } else {
            realClient.findChildPages(pageId, expansions)
        }
    }

    override suspend fun setPageProperty(pageId: String, name: String, value: PagePropertyInput) {
        logger.info { "(dryrun) Setting property on page $pageId: $name=${value.value}, version=${value.version.number}" }
    }

    override suspend fun deleteLabel(pageId: String, label: String) {
        logger.info { "(dryrun) Deleting label on page $pageId: $label" }
    }

    override suspend fun addLabels(pageId: String, labels: List<String>) {
        logger.info { "(dryrun) Adding labels on page $pageId: $labels" }
    }

    override suspend fun addAttachments(
        pageId: String,
        pageAttachmentInput: List<PageAttachmentInput>
    ): PageAttachments {
        pageAttachmentInput.forEach {
            logger.info { "(dryrun) Creating attachment on page $pageId: ${contentDetails(it)}" }
        }
        return PageAttachments(results = pageAttachmentInput.map { toServerAttachment(UNDEFINED_ID, it) })
    }

    override suspend fun updateAttachment(
        pageId: String,
        attachmentId: String,
        pageAttachmentInput: PageAttachmentInput
    ): Attachment {
        logger.info { "(dryrun) Updating attachment $attachmentId on page $pageId: ${contentDetails(pageAttachmentInput)}" }
        return toServerAttachment(attachmentId, pageAttachmentInput)
    }

    private fun toServerAttachment(
        attachmentId: String,
        pageAttachmentInput: PageAttachmentInput
    ) = Attachment(attachmentId, pageAttachmentInput.name, emptyMap())

    private fun contentDetails(pageAttachmentInput: PageAttachmentInput) =
        "uploading ${pageAttachmentInput.content} with contentType=${pageAttachmentInput.contentType}, comment=${pageAttachmentInput.comment}"

    override suspend fun deleteAttachment(attachmentId: String) {
        logger.info { "(dryrun) Deleting attachment $attachmentId" }
    }
}