package com.github.zeldigas.text2confl.cli.upload

import com.github.zeldigas.confclient.*
import com.github.zeldigas.confclient.model.ConfluencePage
import com.github.zeldigas.confclient.model.PageProperty
import com.github.zeldigas.text2confl.cli.config.EditorVersion
import com.github.zeldigas.text2confl.convert.Page
import com.github.zeldigas.text2confl.convert.PageContent
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.nio.file.Path
import kotlin.io.path.extension

internal class PageUploadOperationsImpl(
    val client: ConfluenceClient,
    val uploadMessage: String,
    val notifyWatchers: Boolean,
    val pageContentChangeDetector: ChangeDetector,
    val editorVersion: EditorVersion
) : PageUploadOperations {

    companion object {
        private val logger = KotlinLogging.logger {  }
    }

    override suspend fun createOrUpdatePageContent(page: Page, space: String, parentPageId: String): ServerPage {
        val serverPage = client.getPageOrNull(
            space = space, title = page.title, expansions =
            listOf(
                "metadata.labels",
                "metadata.properties.${HASH_PROPERTY}",
                "metadata.properties.${EDITOR_PROPERTY}",
                "version",
                "children.attachment"
            ) + pageContentChangeDetector.extraData
        )
        return if (serverPage != null) {
            updateExistingPage(serverPage, page, parentPageId)
        } else {
            createNewPage(space, parentPageId, page)
        }
    }

    private suspend fun updateExistingPage(
        serverPage: ConfluencePage,
        page: Page,
        parentPageId: String
    ): ServerPage {
        if (pageContentChangeDetector.strategy(serverPage, page.content)) {
            logger.info { "Page content requires update: ${serverPage.id}, ${serverPage.title}" }
            client.updatePage(
                serverPage.id,
                PageContentInput(
                    parentPageId,
                    page.title,
                    page.content.body,
                    version = serverPage.version?.number!! + 1
                ),
                PageUpdateOptions(notifyWatchers, uploadMessage)
            )
            setPageContentHash(serverPage.id, page.content, serverPage.pageProperty(HASH_PROPERTY))
            setEditorVersion(serverPage.id, serverPage.pageProperty(EDITOR_PROPERTY))
        } else {
            logger.info { "Page is up to date, nothing to do: ${serverPage.id}, ${serverPage.title}" }
        }
        return ServerPage(
            serverPage.id,
            null,
            serverPage.metadata?.labels?.results ?: emptyList(),
            serverPage.children?.attachment?.results ?: emptyList()
        )
    }

    private suspend fun createNewPage(
        space: String,
        parentPageId: String,
        page: Page
    ): ServerPage {
        logger.info { "Page does not exist, need to create it" }
        val serverPage = client.createPage(
            PageContentInput(parentPageId, page.title, page.content.body, space),
            PageUpdateOptions(notifyWatchers, uploadMessage)
        )
        setPageContentHash(serverPage.id, page.content)
        setEditorVersion(serverPage.id)
        return ServerPage(serverPage.id, parentPageId, emptyList(), emptyList())
    }

    private suspend fun setPageContentHash(pageId: String, pageContent: PageContent, pageProperty: PageProperty? = null) {
        setOrUpdateProperty(pageId, pageContent.hash, pageProperty)
    }

    private suspend fun setEditorVersion(pageId: String, pageProperty: PageProperty? = null) {
        setOrUpdateProperty(pageId, editorVersion.propertyValue, pageProperty)
    }

    private suspend fun setOrUpdateProperty(
        pageId: String,
        value: String,
        existingProperty: PageProperty?
    ) {
        if (existingProperty == null) {
            client.setPageProperty(pageId, EDITOR_PROPERTY, PagePropertyInput.newProperty(value))
        } else if (existingProperty.value != value) {
            client.setPageProperty(
                pageId,
                EDITOR_PROPERTY,
                PagePropertyInput.updateOf(existingProperty, value)
            )
        }
    }

    override suspend fun updatePageLabels(serverPage: ServerPage, content: PageContent) {
        val labels = serverPage.labels.map { it.label }
        if (labels != content.labels) {

            val labelsToDelete = labels - content.labels
            labelsToDelete.forEach { client.deleteLabel(serverPage.id, it) }
            val labelsToAdd = content.labels - labels
            if (labels.isNotEmpty()) {
                client.addLabels(serverPage.id, labelsToAdd)
            }
        }
    }

    override suspend fun updatePageAttachments(serverPage: ServerPage, content: PageContent) {
        if (serverPage.attachments.isEmpty() && content.attachments.isEmpty()) return

        val serverAttachments =
            serverPage.attachments.map { it.title to ServerAttachment(
                it.id,
                it.metadata.attachmentHash
            )
            }.toMap()
        val new = content.attachments.filter { it.attachmentName !in serverAttachments }
        val reUpload =
            content.attachments.filter { it.attachmentName in serverAttachments && serverAttachments[it.attachmentName]?.hash != it.hash }
        val extraAttachments =
            serverAttachments.filter { (name, _) -> content.attachments.none { it.attachmentName == name } }

        logger.info { "To upload: $new, to reupload=$reUpload, toDrop: $extraAttachments" }

        coroutineScope {
            if (new.isNotEmpty()) {
                launch {
                    client.addAttachments(
                        serverPage.id,
                        new.map { it.toAttachmentInput() })
                }
            }
            reUpload.forEach {
                launch {
                    client.updateAttachment(
                        serverPage.id, serverAttachments.getValue(it.attachmentName).id, it.toAttachmentInput()
                    )
                }
            }
            extraAttachments.values.forEach {
                launch { client.deleteAttachment(it.id) }
            }
        }
    }

    private data class ServerAttachment(
        val id: String, val hash: String?
    )
}

private val EditorVersion.propertyValue: String
    get() = name.lowercase()

private val PageContent.labels: List<String>
    get() = when (val result = header.attributes["labels"]) {
        is List<*> -> result.map { it.toString() }
        is String -> result.split(",").map { it.trim() }
        else -> emptyList()
    }

private fun ConfluencePage.pageProperty(name: String): PageProperty? {
    return metadata?.properties?.get(name)
}

private val Map<String, Any?>.attachmentHash: String?
    get() {
        val comment = this["comment"] as? String ?: return null
        return """HASH:(\w+)""".toRegex().find(comment)?.groups?.get(1)?.value
    }

private fun com.github.zeldigas.text2confl.convert.Attachment.toAttachmentInput(): PageAttachmentInput =
    PageAttachmentInput(attachmentName, resourceLocation, "HASH:${hash}", resolveContentType(resourceLocation))

private fun resolveContentType(file: Path): String? = CONTENT_TYPES[file.extension.lowercase().trim()]

private val CONTENT_TYPES = mapOf(
    "png" to "image/png",
    "jpg" to "image/jpeg",
    "jpeg" to "image/jpeg",
    "svg" to "image/svg+xml",
    "gif" to "image/gif",
    "bmp" to "image/bmp",
    "txt" to "text/plain",
    "csv" to "text/csv",
    "html" to "text/html",
    "md" to "text/markdown",
    "js" to "text/javascript",
    "doc" to "application/msword",
    "xls" to "application/vnd.ms-excel",
    "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "ppt" to "application/vnd.ms-powerpoint",
    "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "odt" to "application/vnd.oasis.opendocument.text",
    "ods" to "application/vnd.oasis.opendocument.spreadsheet",
    "odp" to "application/vnd.oasis.opendocument.presentation",
    "yaml" to "application/x-yaml",
    "yml" to "application/x-yaml",
    "json" to "application/json",
    "zip" to "application/zip",
    "pdf" to "application/pdf"
)