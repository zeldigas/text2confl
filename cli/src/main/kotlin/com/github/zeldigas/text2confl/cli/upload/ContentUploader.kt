package com.github.zeldigas.text2confl.cli.upload

import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.text2confl.cli.config.EditorVersion
import com.github.zeldigas.text2confl.convert.Page
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging


class ContentUploader(
    val pageUploadOperations: PageUploadOperations
) {

    constructor(
        client: ConfluenceClient,
        uploadMessage: String,
        notifyWatchers: Boolean,
        pageContentChangeDetector: ChangeDetector,
        editorVersion: EditorVersion
    ) : this(PageUploadOperationsImpl(client, uploadMessage, notifyWatchers, pageContentChangeDetector, editorVersion))

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

    private suspend fun uploadPage(page: Page, space: String, parentPageId: String): String {
        val serverPage = pageUploadOperations.createOrUpdatePageContent(page, space, parentPageId)
        pageUploadOperations.updatePageLabels(serverPage, page.content)
        pageUploadOperations.updatePageAttachments(serverPage, page.content)
        return serverPage.id
    }

}