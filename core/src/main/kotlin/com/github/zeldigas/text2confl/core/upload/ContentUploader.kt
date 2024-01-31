package com.github.zeldigas.text2confl.core.upload

import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.confclient.model.ConfluencePage
import com.github.zeldigas.text2confl.convert.EditorVersion
import com.github.zeldigas.text2confl.convert.Page
import com.github.zeldigas.text2confl.convert.PageHeader
import com.github.zeldigas.text2confl.core.config.Cleanup
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.network.sockets.*
import kotlinx.coroutines.*


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

    fun run(pages: List<Page>, space: String, parentPageId: String) = runBlocking {
        withContext(Dispatchers.Default) {
            uploadPages(pages, space, parentPageId)
        }
    }

    fun runBlocking(pages: List<Page>, space: String, parentPageId: String) = runBlocking {
        withContext(Dispatchers.Default) {
            uploadPagesBlocking(pages, space, parentPageId)
        }
    }

    suspend fun uploadPagesBlocking(pages: List<Page>, space: String, parentPageId: String) {
        val uploadedPages = uploadPagesRecursiveBlocking(pages, space, parentPageId)
        logger.info { "Uploaded Pages : " + uploadedPages.size }
        tracker.uploadsCompleted()
        handleOrphans(uploadedPages)
    }

    private suspend fun ContentUploader.handleOrphans(uploadedPages: List<PageUploadResult>) {
        val uploadedPagesByParent = buildOrphanedRemovalRegistry(uploadedPages)
        deleteOrphans(uploadedPagesByParent)
    }

    suspend fun uploadPages(pages: List<Page>, space: String, parentPageId: String) {
        val uploadedPages = uploadPagesRecursive(pages, space, parentPageId)
        tracker.uploadsCompleted()
        handleOrphans(uploadedPages)
    }

    private suspend fun uploadPagesRecursiveBlocking(
        pages: List<Page>,
        space: String,
        parentPageId: String
    ): List<PageUploadResult> {

        val uploadedPages = ArrayList<PageUploadResult>()
        pages.forEach {
            try {
                val result = uploadPage(it, space, parentPageId)
                uploadedPages.addAll(
                    buildList {
                        add(result)
                        if (it.children.isNotEmpty()) {
                            addAll(uploadPagesRecursiveBlocking(it.children, space, result.page.id))
                        }
                    })
            } catch (e: ConnectTimeoutException) {
                logger.error { e.message }
                return emptyList()
            }
        }
        return uploadedPages
    }

    private suspend fun uploadPagesRecursive(
        pages: List<Page>,
        space: String,
        parentPageId: String
    ): List<PageUploadResult> {
        return supervisorScope {
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
            if (pageResult is PageOperationResult.Failed){
                logger.warn { "Failed to upload Page : title=${page.title}, src=${page.source}" }
            } else {
                logger.info { "Page uploaded: title=${page.title}, src=${page.source}: id=${serverPage.id}" }
            }
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
        if (header.parent != null) return client.getPage(space, header.parent!!).id

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
        coroutineScope {
            for ((parent, children) in uploadedPagesByParent) {
                launch { deleteOrphanedChildren(parent, children) }
            }
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