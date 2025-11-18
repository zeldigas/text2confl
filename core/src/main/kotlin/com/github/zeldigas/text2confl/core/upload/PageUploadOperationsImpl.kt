package com.github.zeldigas.text2confl.core.upload

import com.github.zeldigas.confclient.*
import com.github.zeldigas.confclient.model.Attachment
import com.github.zeldigas.confclient.model.ConfluencePage
import com.github.zeldigas.confclient.model.PageProperty
import com.github.zeldigas.text2confl.convert.EditorVersion
import com.github.zeldigas.text2confl.convert.Page
import com.github.zeldigas.text2confl.convert.PageContent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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

    override suspend fun createOrUpdatePageContent(
        page: Page,
        space: String,
        parentPageId: String
    ): PageOperationResult {
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
        space = space, title = page.title, loadOptions =
        setOf(
            SimplePageLoadOptions.Labels,
            SimplePageLoadOptions.Version,
            SimplePageLoadOptions.Attachments,
            SimplePageLoadOptions.ParentId,
            propertyExpansion(HASH_PROPERTY),
            propertyExpansion(EDITOR_PROPERTY),
            propertyExpansion(TENANT_PROPERTY),
        )
                + page.properties.keys.map { propertyExpansion(it) }
                + pageContentChangeDetector.extraOptions
    )

    private suspend fun updateExistingPage(
        confluencePageToUpdate: ConfluencePage,
        page: Page,
        parentPageId: String
    ): PageOperationResult {
        checkNoCycle(parentPageId, confluencePageToUpdate)
        checkTenantBeforeUpdate(confluencePageToUpdate)
        val (renamed, confluencePage) = adjustTitleIfRequired(confluencePageToUpdate, page)

        val serverPageDetails = createServerPage(confluencePage, parentPageId);

        val result = if (pageContentChangeDetector.strategy(confluencePage, page.content)) {
            updatePageContent(confluencePage, parentPageId, page, serverPageDetails)
        } else if (confluencePage.parentId != parentPageId) {
            changePageParent(confluencePage, parentPageId, page, serverPageDetails, confluencePageToUpdate.title)
        } else if (renamed) {
            PageOperationResult.LocationModified(
                page,
                serverPageDetails,
                parentPageId,
                confluencePageToUpdate.title
            )
        } else {
            logger.info { "Page is up to date, nothing to do: ${confluencePage.id}, ${confluencePage.title}" }
            PageOperationResult.NotModified(page, serverPageDetails)
        }
        setPageProperties(page, confluencePage)

        return result
    }

    private suspend fun adjustTitleIfRequired(
        serverPage: ConfluencePage,
        page: Page
    ): Pair<Boolean, ConfluencePage> {
        return if (page.title != serverPage.title) {
            logger.info { "Changing page title: ${serverPage.title} -> ${page.title} " }
            val updated = client.renamePage(serverPage, page.title, PageUpdateOptions(notifyWatchers, uploadMessage))
            true to serverPage.copy(
                title = updated.title,
                version = updated.version,
            )
        } else {
            false to serverPage
        }
    }

    private suspend fun updatePageContent(
        confluencePage: ConfluencePage,
        parentPageId: String,
        page: Page,
        serverPageDetails: ServerPage
    ): PageOperationResult.ContentModified {
        logger.info { "Page content requires update: ${confluencePage.id}, ${confluencePage.title}" }
        client.updatePage(
            confluencePage.id,
            PageContentInput(
                parentPageId,
                page.title,
                page.content.body,
                version = confluencePage.version!!.number + 1
            ),
            PageUpdateOptions(notifyWatchers, uploadMessage)
        )
        return PageOperationResult.ContentModified(
            page,
            serverPageDetails,
            confluencePage.parentId != parentPageId
        )
    }

    private suspend fun changePageParent(
        confluencePage: ConfluencePage,
        parentPageId: String,
        page: Page,
        serverPageDetails: ServerPage,
        originalTitle: String
    ): PageOperationResult.LocationModified {
        changeParent(confluencePage, parentPageId)
        return PageOperationResult.LocationModified(
            page,
            serverPageDetails,
            confluencePage.parentId ?: "",
            originalTitle
        )
    }

    private fun checkNoCycle(parentPageId: String, pageToUpdate: ConfluencePage) {
        if (pageToUpdate.id == parentPageId) {
            throw PageCycleException(parentPageId, pageToUpdate.title)
        }
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
            space, title, loadOptions = setOf(
                SimplePageLoadOptions.ParentId, SimplePageLoadOptions.Version,
                PagePropertyLoad(TENANT_PROPERTY)
            )
        ) ?: throw PageNotFoundException(space, title)
        if (serverPage.parentId != parentId) {
            checkTenantBeforeUpdate(serverPage)
            changeParent(serverPage, parentId)
        }
        return createServerPage(serverPage, parentId)
    }

    private suspend fun changeParent(
        serverPage: ConfluencePage,
        parentPageId: String
    ) {
        logger.info { "Changing page parent from ${serverPage.parentId} to $parentPageId" }
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
        serverPage.labels ?: emptyList(),
        serverPage.attachments?.let { client.fetchAllAttachments(it) } ?: emptyList(),
        serverPage.links
    )

    private suspend fun createNewPage(
        space: String,
        parentPageId: String,
        page: Page
    ): PageOperationResult.Created {
        logger.info { "Page does not exist, need to create it: ${page.title}" }
        val serverPage = client.createPage(
            PageContentInput(parentPageId, page.title, page.content.body, space),
            PageUpdateOptions(notifyWatchers, uploadMessage),
            loadOptions = setOf(
                SimplePageLoadOptions.Labels,
                SimplePageLoadOptions.Version,
//                SimplePageLoadOptions.Attachments,
                PagePropertyLoad(HASH_PROPERTY),
                PagePropertyLoad(EDITOR_PROPERTY),
            )
        )
        setPageProperties(page, serverPage)
        return PageOperationResult.Created(page, createServerPage(serverPage, parentPageId))
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

    override suspend fun updatePageLabels(serverPage: ServerPage, content: PageContent): LabelsUpdateResult {
        val labels = serverPage.labels.map { it.label ?: it.name }
        return if (labels != content.labels) {

            val labelsToDelete = labels - content.labels
            labelsToDelete.forEach { client.deleteLabel(serverPage.id, it) }
            val labelsToAdd = content.labels - labels
            if (labelsToAdd.isNotEmpty()) {
                client.addLabels(serverPage.id, labelsToAdd)
            }
            LabelsUpdateResult.Updated(labelsToAdd, labelsToDelete)
        } else {
            LabelsUpdateResult.NotChanged
        }
    }

    override suspend fun updatePageAttachments(serverPage: ServerPage, content: PageContent): AttachmentsUpdateResult {
        if (serverPage.attachments.isEmpty() && content.attachments.isEmpty()) return AttachmentsUpdateResult.NotChanged

        val serverAttachments =
            serverPage.attachments.map {
                it.title to ServerAttachment(
                    it,
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
        return if (new.isEmpty() && reUpload.isEmpty() && extraAttachments.isEmpty()) {
            AttachmentsUpdateResult.NotChanged
        } else {
            AttachmentsUpdateResult.Updated(new, reUpload, extraAttachments.values.map { it.attachment })
        }
    }

    override suspend fun findChildPages(pageId: String): List<ConfluencePage> {
        return client.findChildPages(
            pageId,
            setOf(propertyExpansion(HASH_PROPERTY), propertyExpansion(TENANT_PROPERTY))
        )
    }

    override suspend fun deletePageWithChildren(page: ConfluencePage): List<ConfluencePage> {
        val deletedPages = mutableListOf(page)
        coroutineScope {
            for (subpage in client.findChildPages(page.id)) {
                launch {
                    deletedPages.addAll(deletePageWithChildren(subpage))
                }
            }
        }
        client.deletePage(page.id)
        return deletedPages
    }

    private data class ServerAttachment(
        val attachment: Attachment, val hash: String?
    ) {
        val id: String
            get() = attachment.id
    }

    private fun propertyExpansion(property: String) = PagePropertyLoad(property)
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