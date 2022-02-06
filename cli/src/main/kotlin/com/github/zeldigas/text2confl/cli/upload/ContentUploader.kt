package com.github.zeldigas.text2confl.cli.upload

import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.text2confl.cli.config.EditorVersion
import com.github.zeldigas.text2confl.convert.Page
import com.github.zeldigas.text2confl.convert.PageHeader
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging


class ContentUploader(
    val pageUploadOperations: PageUploadOperations,
    val client: ConfluenceClient
) {

    constructor(
        client: ConfluenceClient,
        uploadMessage: String,
        notifyWatchers: Boolean,
        pageContentChangeDetector: ChangeDetector,
        editorVersion: EditorVersion
    ) : this(PageUploadOperationsImpl(client, uploadMessage, notifyWatchers, pageContentChangeDetector, editorVersion), client)

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    suspend fun uploadPages(pages: List<Page>, space: String, parentPageId: String) {
        coroutineScope {
            for (page in pages) {
                launch {
                    logger.info { "Uploading page: title=${page.title}" }
                    val pageId = uploadPage(page, space, parentPageId)
                    logger.debug { "Page uploaded: title=${page.title}, id=$pageId" }
                    if (page.children.isNotEmpty()) {
                        uploadPages(page.children, space, pageId)
                    }
                }
            }
        }
    }

    private suspend fun uploadPage(page: Page, space: String, defaultParentPage: String): String {
        val parentId = customPageParent(page, space) ?: defaultParentPage
        val serverPage = pageUploadOperations.createOrUpdatePageContent(page, space, parentId)
        pageUploadOperations.updatePageLabels(serverPage, page.content)
        pageUploadOperations.updatePageAttachments(serverPage, page.content)
        return serverPage.id
    }

    private suspend fun customPageParent(page: Page, space: String): String? {
        val header = page.content.header

        if (header.parentId != null) return header.parentId
        if (header.parent != null) return client.getPage(space, header.parent!!).id

        return null
    }

}

private val PageHeader.parentId: String?
    get()  = attributes["parentId"]?.toString()

private val PageHeader.parent: String?
    get()  = attributes["parent"]?.toString()