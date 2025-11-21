package com.github.zeldigas.text2confl.core.upload

import com.github.zeldigas.confclient.PageLoadOptions
import com.github.zeldigas.confclient.SimplePageLoadOptions
import com.github.zeldigas.confclient.model.Attachment
import com.github.zeldigas.confclient.model.ConfluencePage
import com.github.zeldigas.confclient.model.Label
import com.github.zeldigas.text2confl.convert.Page
import com.github.zeldigas.text2confl.convert.PageContent
import java.nio.file.Path

const val HASH_PROPERTY = "contenthash"
const val TENANT_PROPERTY = "t2ctenant"
const val EDITOR_PROPERTY = "editor"

interface PageUploadOperations {

    suspend fun createOrUpdatePageContent(page: Page, space: String, parentPageId: String): PageOperationResult

    suspend fun checkPageAndUpdateParentIfRequired(title: String, space: String, parentId: String): ServerPage

    suspend fun updatePageLabels(serverPage: ServerPage, content: PageContent): LabelsUpdateResult

    suspend fun updatePageAttachments(serverPage: ServerPage, content: PageContent): AttachmentsUpdateResult

    suspend fun findChildPages(pageId: String): List<ConfluencePage>
    suspend fun deletePageWithChildren(page: ConfluencePage): List<ConfluencePage>

}

sealed class PageOperationResult {
    data class NotModified(override val local: Page, override val serverPage: ServerPage) : PageOperationResult()
    data class LocationModified(
        override val local: Page,
        override val serverPage: ServerPage,
        val previousParent: String,
        val previousTitle: String
    ) : PageOperationResult()

    data class Created(override val local: Page, override val serverPage: ServerPage) : PageOperationResult()
    data class ContentModified(
        override val local: Page,
        override val serverPage: ServerPage,
        val parentChanged: Boolean = false
    ) : PageOperationResult()

    abstract val local: Page
    abstract val serverPage: ServerPage
}

sealed class LabelsUpdateResult {
    data class Updated(val added: List<String> = emptyList(), val removed: List<String> = emptyList()) :
        LabelsUpdateResult()

    object NotChanged : LabelsUpdateResult()
}

sealed class AttachmentsUpdateResult {
    data class Updated(
        val added: List<com.github.zeldigas.text2confl.convert.Attachment> = emptyList(),
        val modified: List<com.github.zeldigas.text2confl.convert.Attachment> = emptyList(),
        val removed: List<Attachment> = emptyList()
    ) : AttachmentsUpdateResult()

    object NotChanged : AttachmentsUpdateResult()
}

abstract class PageOperationException(message: String, cause: Exception? = null) : RuntimeException(message, cause)

data class PageNotFoundException(val space: String, val title: String, val processedFile: Path? = null) :
    PageOperationException("Page '$title' in space '$space' not found" + (processedFile?.let { " when processing $it" } ?: ""))

data class PageCycleException(val parentId: String, val title: String) :
    PageOperationException("Can't publish page '$title' with parent $parentId - this is id of page itself")

enum class ChangeDetector(
    val extraOptions: Set<PageLoadOptions>,
    val strategy: (serverPage: ConfluencePage, content: PageContent) -> Boolean
) {
    HASH(emptySet(), { serverPage, content ->
        serverPage.pageProperty(HASH_PROPERTY)?.value != content.hash
    }),
    CONTENT(setOf(SimplePageLoadOptions.Content), { serverPage, content ->
        serverPage.body?.storage?.value != content.body
    })
}

data class ServerPage(
    val id: String, val title: String, val parent: String, val labels: List<Label>, val attachments: List<Attachment>,
    val links: Map<String, String> = emptyMap()
)

class InvalidTenantException(page: String, expected: String?, actual: String?) :
    RuntimeException("Page $page must be in tenant \"${tenant(expected)}\" but actual is \"${tenant(actual)}\"")

private fun tenant(value: String?): String = value ?: "(no tenant)"
