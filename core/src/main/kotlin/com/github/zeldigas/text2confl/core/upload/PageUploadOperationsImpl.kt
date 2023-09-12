package com.github.zeldigas.text2confl.core.upload

import com.github.zeldigas.confclient.*
import com.github.zeldigas.confclient.model.ConfluencePage
import com.github.zeldigas.confclient.model.PageProperty
import com.github.zeldigas.text2confl.convert.EditorVersion
import com.github.zeldigas.text2confl.convert.Page
import com.github.zeldigas.text2confl.convert.PageContent
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.io.path.extension

internal class PageUploadOperationsImpl(
        val client: ConfluenceClient,
        val uploadMessage: String,
        val notifyWatchers: Boolean,
        val pageContentChangeDetector: ChangeDetector,
        val editorVersion: EditorVersion,
        val tenant: String? = null
) : PageUploadOperations {

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    override suspend fun createOrUpdatePageContent(page: Page, space: String, parentPageId: String): ServerPage {
        val serverPage = findPageOnServer(space, page)
        return if (serverPage != null) {
            updateExistingPage(serverPage, page, parentPageId)
        } else {
            createNewPage(space, parentPageId, page)
        }
    }

    private suspend fun findPageOnServer(
        space: String,
        page: Page
    ) = client.getPageOrNull(
        space = space, title = page.title, expansions =
        setOf(
            "metadata.labels",
            propertyExpansion(HASH_PROPERTY),
            propertyExpansion(EDITOR_PROPERTY),
            propertyExpansion(TENANT_PROPERTY),
            "version",
            "children.attachment",
            "ancestors"
        )
                + page.properties.keys.map { propertyExpansion(it) }
                + pageContentChangeDetector.extraData
    )

    private suspend fun updateExistingPage(
        serverPage: ConfluencePage,
        page: Page,
        parentPageId: String
    ): ServerPage {
        checkTenantBeforeUpdate(serverPage)
        if (pageContentChangeDetector.strategy(serverPage, page.content)) {
            logger.info { "Page content requires update: ${serverPage.id}, ${serverPage.title}" }
            client.updatePage(
                serverPage.id,
                PageContentInput(
                    parentPageId,
                    page.title,
                    page.content.body,
                    version = serverPage.version!!.number + 1
                ),
                PageUpdateOptions(notifyWatchers, uploadMessage)
            )
        } else if (serverPage.parent?.id != parentPageId) {
            changeParent(serverPage, parentPageId)
        } else {
            logger.info { "Page is up to date, nothing to do: ${serverPage.id}, ${serverPage.title}" }
        }
        setPageProperties(page, serverPage)
        return createServerPage(serverPage, parentPageId)
    }

    private fun checkTenantBeforeUpdate(serverPage: ConfluencePage) {
        val pageTenant = serverPage.pageProperty(TENANT_PROPERTY)?.value ?: return

        if (pageTenant !is String) throw IllegalStateException("$TENANT_PROPERTY property is not a string")

        if (pageTenant != tenant) {
            throw InvalidTenantException(serverPage.title, tenant, pageTenant)
        }
    }

    override suspend fun checkPageAndUpdateParentIfRequired(
        title: String,
        space: String,
        parentId: String
    ): ServerPage {
        val serverPage = client.getPageOrNull(
            space, title, expansions = setOf(
                "ancestors", "version", propertyExpansion(TENANT_PROPERTY)
            )
        ) ?: throw IllegalStateException("Page not found in $space: $title")
        if (serverPage.parent?.id != parentId) {
            checkTenantBeforeUpdate(serverPage)
            changeParent(serverPage, parentId)
        }
        return createServerPage(serverPage, parentId)
    }

    private suspend fun changeParent(
        serverPage: ConfluencePage,
        parentPageId: String
    ) {
        logger.info { "Changing page parent from ${serverPage.parent?.id} to $parentPageId" }
        client.changeParent(
            serverPage.id,
            serverPage.title,
            serverPage.version!!.number + 1,
            parentPageId,
            PageUpdateOptions(notifyWatchers, uploadMessage)
        )
    }

    private suspend fun createServerPage(
        serverPage: ConfluencePage,
        parentPageId: String
    ) = ServerPage(
        serverPage.id,
        serverPage.title,
        parentPageId,
        serverPage.metadata?.labels?.results ?: emptyList(),
        serverPage.children?.attachment?.let { client.fetchAllAttachments(it) } ?: emptyList()
    )

    private suspend fun createNewPage(
        space: String,
        parentPageId: String,
        page: Page
    ): ServerPage {
        logger.info { "Page does not exist, need to create it: ${page.title}" }
        val serverPage = client.createPage(
            PageContentInput(parentPageId, page.title, page.content.body, space),
            PageUpdateOptions(notifyWatchers, uploadMessage),
            expansions = listOf(
                "metadata.labels",
                "metadata.properties.$HASH_PROPERTY",
                "metadata.properties.$EDITOR_PROPERTY",
                "version",
                "children.attachment"
            )
        )
        setPageProperties(page, serverPage)
        return createServerPage(serverPage, parentPageId)
    }

    private suspend fun setPageProperties(
        page: Page,
        serverPage: ConfluencePage
    ) {
        val allProperties = mapOf(
            HASH_PROPERTY to page.content.hash,
            EDITOR_PROPERTY to editorVersion.propertyValue,
        ) + tenantProperty() + page.properties.filterKeys { it != HASH_PROPERTY }
        allProperties.forEach { (name, value) ->
            setOrUpdateProperty(
                serverPage.id,
                propertyName = name,
                value = value,
                existingProperty = serverPage.pageProperty(name)
            )
        }
    }

    private fun tenantProperty(): Map<String, Any> = tenant?.let { mapOf(TENANT_PROPERTY to it) } ?: emptyMap()

    private suspend fun setOrUpdateProperty(
        pageId: String,
        propertyName: String,
        value: Any,
        existingProperty: PageProperty?
    ) {
        if (existingProperty == null) {
            client.setPageProperty(pageId, propertyName, PagePropertyInput.newProperty(value))
        } else if (existingProperty.value != value) {
            client.setPageProperty(
                pageId,
                propertyName,
                PagePropertyInput.updateOf(existingProperty, value)
            )
        }
    }

    override suspend fun updatePageLabels(serverPage: ServerPage, content: PageContent) {
        val labels = serverPage.labels.map { it.label ?: it.name }
        if (labels != content.labels) {

            val labelsToDelete = labels - content.labels
            labelsToDelete.forEach { client.deleteLabel(serverPage.id, it) }
            val labelsToAdd = content.labels - labels
            if (labelsToAdd.isNotEmpty()) {
                client.addLabels(serverPage.id, labelsToAdd)
            }
        }
    }

    override suspend fun updatePageAttachments(serverPage: ServerPage, content: PageContent) {
        if (serverPage.attachments.isEmpty() && content.attachments.isEmpty()) return

        val serverAttachments =
            serverPage.attachments.map {
                it.title to ServerAttachment(
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
        }
        reUpload.forEach {
            client.updateAttachment(
                serverPage.id, serverAttachments.getValue(it.attachmentName).id, it.toAttachmentInput()
            )
        }
        extraAttachments.values.forEach {
            client.deleteAttachment(it.id)
        }
    }

    override suspend fun findChildPages(pageId: String): List<ConfluencePage> {
        return client.findChildPages(
            pageId,
            listOf(propertyExpansion(HASH_PROPERTY), propertyExpansion(TENANT_PROPERTY))
        );
    }

    override suspend fun deletePageWithChildren(pageId: String) {
        coroutineScope {
            for (subpage in client.findChildPages(pageId)) {
                launch {
                    deletePageWithChildren(subpage.id)
                }
            }
        }
        client.deletePage(pageId)
    }

    private data class ServerAttachment(
        val id: String, val hash: String?
    )

    private fun propertyExpansion(property: String) = "metadata.properties.$property"
}

private val EditorVersion.propertyValue: String
    get() = name.lowercase()

private val PageContent.labels: List<String>
    get() = header.pageLabels

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