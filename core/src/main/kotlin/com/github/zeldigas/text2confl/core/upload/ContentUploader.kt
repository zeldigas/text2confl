package com.github.zeldigas.text2confl.core.upload

import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.confclient.model.ConfluencePage
import com.github.zeldigas.text2confl.convert.EditorVersion
import com.github.zeldigas.text2confl.convert.Page
import com.github.zeldigas.text2confl.convert.PageHeader
import com.github.zeldigas.text2confl.core.config.Cleanup
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch


class ContentUploader(
    val pageUploadOperations: PageUploadOperations,
    val client: ConfluenceClient,
    val cleanup: Cleanup,
    val tenant: String?,
    val tracker: UploadOperationTracker = NOP
) {

    constructor(
        client: ConfluenceClient,
        uploadMessage: String,
        notifyWatchers: Boolean,
        pageContentChangeDetector: ChangeDetector,
        editorVersion: EditorVersion,
        cleanup: Cleanup,
        tenant: String?,
        tracker: UploadOperationTracker = NOP
    ) : this(
        PageUploadOperationsImpl(
            client,
            uploadMessage,
            notifyWatchers,
            pageContentChangeDetector,
            editorVersion,
            tenant
        ),
        client,
        cleanup,
        tenant,
        tracker
    )

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    suspend fun uploadPages(pages: List<Page>, space: String, parentPageId: String) {
        val uploadedPages = uploadPagesRecursive(pages, space, parentPageId)
        tracker.uploadsCompleted()
        val uploadedPagesByParent = buildOrphanedRemovalRegistry(uploadedPages)
        deleteOrphans(uploadedPagesByParent)
    }

    private suspend fun uploadPagesRecursive(
        pages: List<Page>,
        space: String,
        parentPageId: String
    ): List<PageUploadResult> {
        return coroutineScope {
            pages.map { page ->
                async {
                    val result = uploadPage(page, space, parentPageId)
                    buildList {
                        add(result)
                        if (page.children.isNotEmpty()) {
                            addAll(uploadPagesRecursive(page.children, space, result.page.id))
                        }
                    }
                }
            }.awaitAll().flatten()
        }
    }

    private suspend fun uploadPage(page: Page, space: String, defaultParentPage: String): PageUploadResult {
        val parentId = customPageParent(page, space) ?: defaultParentPage
        return if (!page.virtual) {
            logger.info { "Uploading page: title=${page.title}, src=${page.source}" }
            val pageResult = pageUploadOperations.createOrUpdatePageContent(page, space, parentId)
            val serverPage = pageResult.serverPage
            val labelUpdate = pageUploadOperations.updatePageLabels(serverPage, page.content)
            val attachmentsUpdated = pageUploadOperations.updatePageAttachments(serverPage, page.content)
            tracker.pageUpdated(pageResult, labelUpdate, attachmentsUpdated)
            logger.info { "Page uploaded: title=${page.title}, src=${page.source}: id=${serverPage.id}" }
            PageUploadResult(parentId, serverPage, virtual = false)
        } else {
            logger.info { "Checking that virtual page exists and properly located: ${page.title}" }
            val virtualPage = try {
                pageUploadOperations.checkPageAndUpdateParentIfRequired(page.title, space, parentId)
            } catch (ex: PageNotFoundException) {
                throw VirtualPageNotFound(page.source, page.title, space)
            }
            PageUploadResult(parentId, virtualPage, true)
        }
    }

    private suspend fun customPageParent(page: Page, space: String): String? {
        val header = page.content.header

        if (header.parentId != null) return header.parentId
        if (header.parent != null) return try {
            client.getPage(space, header.parent!!).id
        } catch (_: com.github.zeldigas.confclient.PageNotFoundException) {
            throw PageNotFoundException(space, header.parent!!, page.source)
        }

        return null
    }

    private fun buildOrphanedRemovalRegistry(
        uploadedPages: List<PageUploadResult>
    ): Map<String, List<ServerPage>> {
        val nonLeafPages =
            uploadedPages.groupBy { it.parentId }.mapValues { (_, v) -> v.map { (_, serverPage) -> serverPage } }
        val leafPages = uploadedPages.asSequence().map { (_, serverPage) -> serverPage.id }
            .filter { it !in nonLeafPages }.map { it to emptyList<ServerPage>() }.toMap()
        return nonLeafPages + leafPages
    }

    private suspend fun deleteOrphans(uploadedPagesByParent: Map<String, List<ServerPage>>) {
        logger.debug { "Running cleanup operation using strategy: $cleanup" }
        logger.debug { "Cleanup operation: $cleanup" }
        try {
            coroutineScope {
                for ((parent, children) in uploadedPagesByParent) {
                    launch { deleteOrphanedChildren(parent, children) }
                }
            }
        } catch (ex: Exception) {
            throw ContentCleanupException("Failed to cleanup orphaned pages using $cleanup strategy", ex)
        }
    }

    private suspend fun deleteOrphanedChildren(pageId: String, children: List<ServerPage>) {
        if (cleanup == Cleanup.None) return

        val managedTitles = children.map { it.title }.toSet()

        val pagesForDeletion = pageUploadOperations.findChildPages(pageId)
            .filter { it.title !in managedTitles }
            .filter { cleanup == Cleanup.All || it.managedPage && sameTenant(it) }

        coroutineScope {
            for (page in pagesForDeletion) {
                launch {
                    logger.info { "Deleting orphaned page and subpages: title=${page.title}, id=${page.id}" }
                    val deletedPages = pageUploadOperations.deletePageWithChildren(page)
                    tracker.pagesDeleted(page, deletedPages)
                }
            }
        }
    }

    private fun sameTenant(it: ConfluencePage) =
        it.pageProperty(TENANT_PROPERTY)?.value == tenant

    private data class PageUploadResult(val parentId: String, val page: ServerPage, val virtual: Boolean)

}

private val PageHeader.parentId: String?
    get() = attributes["parentId"]?.toString()

private val PageHeader.parent: String?
    get() = attributes["parent"]?.toString()