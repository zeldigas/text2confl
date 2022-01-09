package com.github.zeldigas.text2confl.cli

import com.github.zeldigas.confclient.*
import com.github.zeldigas.confclient.model.Attachment
import com.github.zeldigas.confclient.model.ConfluencePage
import com.github.zeldigas.confclient.model.Label
import com.github.zeldigas.confclient.model.PageProperty
import com.github.zeldigas.text2confl.cli.config.EditorVersion
import com.github.zeldigas.text2confl.convert.Page
import com.github.zeldigas.text2confl.convert.PageContent
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.nio.file.Path
import kotlin.io.path.extension


private val EditorVersion.propertyValue: String
    get() = name.lowercase()

private val PageContent.labels: List<String>
    get() = when (val result = header.attributes["labels"]) {
        is List<*> -> result as List<String>
        is String -> result.split(",").map { it.trim() }
        else -> emptyList()
    }


enum class ChangeDetector(
    val extraData: List<String>,
    val strategy: (serverPage: ConfluencePage, content: PageContent) -> Boolean
) {
    HASH(emptyList(), { serverPage, content ->
        serverPage.metadata?.properties?.get(ContentUploader.HASH_PROPERTY)?.value != content.hash
    }),
    CONTENT(listOf("body.storage"), { serverPage, content ->
        serverPage.body?.storage?.value != content.body
    })
}


class ContentUploader(
    val client: ConfluenceClient,
    val uploadMessage: String,
    val notifyWatchers: Boolean,
    val pageContentChangeDetector: ChangeDetector,
    val editorVersion: EditorVersion
) {

    companion object {
        const val HASH_PROPERTY = "content-hash"
        const val EDITOR_PROPERTY = "editor"
        private val logger = KotlinLogging.logger {}
    }

    suspend fun uploadPages(pages: List<Page>, space: String, parentPageId: String) {
        coroutineScope {
            for (page in pages) {
                launch {
                    logger.info { "Uploading page: ${page.title}" }
                    val pageId = uploadPage(page, space, parentPageId)
                    uploadPages(page.children, space, pageId)
                }
            }
        }
    }

    private suspend fun uploadPage(page: Page, space: String, parentPageId: String): String {
        val serverPage = createOrUpdatePageContent(page, space, parentPageId)
        updatePageLabels(serverPage, page.content)
        updatePageAttachments(serverPage, page.content)
        return serverPage.id
    }

    private suspend fun createOrUpdatePageContent(page: Page, space: String, parentPageId: String): ServerPage {
        val serverPage = client.getPageOrNull(
            space = space, title = page.title, expansions =
            listOf(
                "metadata.labels",
                "metadata.properties.$HASH_PROPERTY",
                "metadata.properties.$EDITOR_PROPERTY",
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
            client.setPageProperty(
                serverPage.id,
                HASH_PROPERTY,
                hashProperty(serverPage.pageProperty(HASH_PROPERTY), page.content.hash)
            )
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

    private fun hashProperty(pageProperty: PageProperty?, hash: String): PagePropertyInput {
        return if (pageProperty == null) {
            PagePropertyInput.newProperty(hash)
        } else {
            PagePropertyInput.updateOf(pageProperty, hash)
        }
    }

    private suspend fun setContentHash(id: String, pageProperty: PageProperty? = null, hash:String) {
        if (pageProperty == null) {
            client.setPageProperty(id, HASH_PROPERTY, PagePropertyInput.newProperty(hash))
        } else if (pageProperty.value != editorVersion.propertyValue) {
            client.setPageProperty(id, EDITOR_PROPERTY, PagePropertyInput.updateOf(pageProperty, editorVersion.propertyValue))
        }
    }

    private suspend fun setEditorVersion(id: String, pageProperty: PageProperty? = null) {
        setOrUpdateProperty(pageProperty, id)
    }

    private suspend fun setOrUpdateProperty(
        pageProperty: PageProperty?,
        id: String
    ) {
        if (pageProperty == null) {
            client.setPageProperty(id, EDITOR_PROPERTY, PagePropertyInput.newProperty(editorVersion.propertyValue))
        } else if (pageProperty.value != editorVersion.propertyValue) {
            client.setPageProperty(
                id,
                EDITOR_PROPERTY,
                PagePropertyInput.updateOf(pageProperty, editorVersion.propertyValue)
            )
        }
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
        client.setPageProperty(serverPage.id, HASH_PROPERTY, PagePropertyInput.newProperty(page.content.hash))
        setEditorVersion(serverPage.id)
        return ServerPage(serverPage.id, parentPageId, emptyList(), emptyList())
    }

    private suspend fun updatePageLabels(serverPage: ServerPage, content: PageContent) {
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

    private suspend fun updatePageAttachments(serverPage: ServerPage, content: PageContent) {
        if (serverPage.attachments.isEmpty() && content.attachments.isEmpty()) return

        val serverAttachments =
            serverPage.attachments.map { it.title to ServerAttachment(it.id, it.metadata.attachmentHash) }.toMap()
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

    private data class ServerPage(
        val id: String, val parent: String?, val labels: List<Label>, val attachments: List<Attachment>
    )

    private data class ServerAttachment(
        val id: String, val hash: String?
    )

}

private fun com.github.zeldigas.text2confl.convert.Attachment.toAttachmentInput(): PageAttachmentInput =
    PageAttachmentInput(attachmentName, resourceLocation, "HASH:${hash}", resolveContentType(resourceLocation))

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

private fun resolveContentType(file:Path) : String? = CONTENT_TYPES[file.extension.lowercase().trim()]

private fun ConfluencePage.pageProperty(name: String): PageProperty? {
    return metadata?.properties?.get(name)
}

private val Map<String, Any?>.attachmentHash: String?
    get() {
        val comment = this["comment"] as? String ?: return null
        return """HASH:(\w+)""".toRegex().find(comment)?.groups?.get(1)?.value
    }