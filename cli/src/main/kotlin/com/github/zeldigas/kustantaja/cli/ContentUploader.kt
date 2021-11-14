package com.github.zeldigas.kustantaja.cli

import com.github.zeldigas.kustantaja.convert.Page
import com.github.zeldigas.kustantaja.convert.PageContent
import mu.KotlinLogging
import org.sahli.asciidoc.confluence.publisher.client.http.ConfluenceClient
import org.sahli.asciidoc.confluence.publisher.client.http.ConfluencePage
import org.sahli.asciidoc.confluence.publisher.client.http.NotFoundException

private val PageContent.labels: List<String>
    get() = when (val result = header.attributes["labels"]) {
        is List<*> -> result as List<String>
        is String -> result.split(",").map { it.trim() }
        else -> emptyList()
    }

class ContentUploader(
    private val client: ConfluenceClient,
    private val uploadMessage: String,
    private val notifyWatchers: Boolean
) {

    companion object {
        const val HASH_PROPERTY = "content-hash"
        private val logger = KotlinLogging.logger {}
    }

    fun uploadPages(pages: List<Page>, space: String, parentPageId: String) {
        pages.forEach { page ->
            logger.info { "Uploading page: ${page.title}" }
            val uploadedPage = uploadPage(page, space, parentPageId)
            uploadPages(page.children, space, uploadedPage)
        }
    }

    private fun uploadPage(page: Page, space: String, parentPageId: String): String {
        val id = updatePageContent(page, space, parentPageId)
        updatePageLabels(id, page.content)
        updatePageAttachments(id, page.content)
        return id
    }

    private fun updatePageAttachments(id: String, content: PageContent) {

    }

    private fun updatePageLabels(id: String, content: PageContent) {
        val labels = client.getLabels(id)
        if (labels != content.labels) {

            val labelsToDelete = labels - content.labels
            labelsToDelete.forEach { client.deleteLabel(id, it) }
            val labelsToAdd = content.labels - labels
            client.addLabels(id, labelsToAdd)
        }
    }

    private fun updatePageContent(page: Page, space: String, parentPageId: String): String {
        return try {
            val id = client.getPageByTitle(space, page.title)
            val serverPage = client.getPageWithContentAndVersionById(id)
            if (updateRequired(serverPage, page.content)) {
                logger.info { "Page content requires update" }
                client.deletePropertyByKey(
                    serverPage.contentId,
                    HASH_PROPERTY
                ) //todo remove this after switching to more advanced client
                client.updatePage(
                    serverPage.contentId,
                    "",
                    page.title,
                    page.content.body,
                    serverPage.version + 1,
                    uploadMessage,
                    notifyWatchers
                )
                client.setPropertyByKey(serverPage.contentId, HASH_PROPERTY, page.content.hash)
            } else {
                logger.info { "Page is up to date, nothing to do" }
            }
            id
        } catch (ex: NotFoundException) {
            logger.info { "Page does not exist, need to create it" }
            val id = client.addPageUnderAncestor(space, parentPageId, page.title, page.content.body, uploadMessage)
            client.setPropertyByKey(id, HASH_PROPERTY, page.content.hash)
            id
        }
    }

    private fun updateRequired(serverPage: ConfluencePage, content: PageContent): Boolean {
        val hash = client.getPropertyByKey(serverPage.contentId, HASH_PROPERTY)
        return hash != content.hash
    }

}