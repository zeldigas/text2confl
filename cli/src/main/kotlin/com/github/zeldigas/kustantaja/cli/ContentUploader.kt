package com.github.zeldigas.kustantaja.cli

import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.confclient.PageContentInput
import com.github.zeldigas.confclient.PagePropertyInput
import com.github.zeldigas.confclient.UpdateParameters
import com.github.zeldigas.confclient.model.Attachment
import com.github.zeldigas.confclient.model.ConfluencePage
import com.github.zeldigas.confclient.model.Label
import com.github.zeldigas.confclient.model.PageProperty
import com.github.zeldigas.kustantaja.convert.Page
import com.github.zeldigas.kustantaja.convert.PageContent
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging


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
    private val client: ConfluenceClient,
    private val uploadMessage: String,
    private val notifyWatchers: Boolean,
    private val pageContentChangeDetector: ChangeDetector
) {

    companion object {
        const val HASH_PROPERTY = "content-hash"
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
                "metadata.properties.${HASH_PROPERTY}",
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
            logger.info { "Page content requires update" }
            client.updatePage(
                serverPage.id,
                PageContentInput(
                    parentPageId,
                    page.title,
                    page.content.body,
                    version = serverPage.version?.number!! + 1
                ),
                UpdateParameters(notifyWatchers, uploadMessage)
            )
            client.setPageProperty(
                serverPage.id,
                HASH_PROPERTY,
                hashProperty(serverPage.pageProperty(HASH_PROPERTY), page.content.hash)
            )
        } else {
            logger.info { "Page is up to date, nothing to do" }
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

    private suspend fun createNewPage(
        space: String,
        parentPageId: String,
        page: Page
    ): ServerPage {
        logger.info { "Page does not exist, need to create it" }
        val serverPage = client.createPage(
            PageContentInput(parentPageId, page.title, page.content.body, space),
            UpdateParameters(notifyWatchers, uploadMessage)
        )
        client.setPageProperty(serverPage.id, HASH_PROPERTY, PagePropertyInput.newProperty(page.content.hash))
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

    private fun updatePageAttachments(serverPage: ServerPage, content: PageContent) {
        if (serverPage.attachments.isEmpty() && content.attachments.isEmpty()) return

        val serverAttachments = serverPage.attachments.map { it.title to ServerAttachment(it.id, it.metadata.attachmentHash)}.toMap()
        val new = content.attachments.filter { it.name !in serverAttachments }
        val reUpload = content.attachments.filter { it.name in serverAttachments && serverAttachments[it.name]?.hash != it.hash }
        val extraAttachments = serverAttachments.keys.filter { name -> content.attachments.none { it.name == name } }

        logger.info { "To upload: $new, to reupload=$reUpload, toDrop: $extraAttachments" }
        //todo upload and delete
    }

    private data class ServerPage(
        val id: String, val parent: String?, val labels: List<Label>, val attachments: List<Attachment>
    )

    private data class ServerAttachment(
        val id: String, val hash: String?
    )

}

private fun ConfluencePage.pageProperty(name: String): PageProperty? {
    return metadata?.properties?.get(name)
}

private val Map<String, String>.attachmentHash: String?
    get() {
        val comment = this["comment"] ?: return null
        return """HASH:(\w+)""".toRegex().find(comment)?.groups?.get(1)?.value
    }