package com.github.zeldigas.text2confl.core.upload

import com.github.zeldigas.confclient.model.Attachment
import com.github.zeldigas.confclient.model.ConfluencePage
import com.github.zeldigas.confclient.model.Label
import com.github.zeldigas.text2confl.convert.Page
import com.github.zeldigas.text2confl.convert.PageContent

const val HASH_PROPERTY = "contenthash"
const val TENANT_PROPERTY = "t2ctenant"
const val EDITOR_PROPERTY = "editor"

interface PageUploadOperations {

    suspend fun createOrUpdatePageContent(page: Page, space: String, parentPageId: String): ServerPage

    suspend fun checkPageAndUpdateParentIfRequired(title: String, space: String, parentId: String): ServerPage

    suspend fun updatePageLabels(serverPage: ServerPage, content: PageContent)

    suspend fun updatePageAttachments(serverPage: ServerPage, content: PageContent)
    suspend fun findChildPages(pageId: String): List<ConfluencePage>
    suspend fun deletePageWithChildren(pageId: String)

}

enum class ChangeDetector(
    val extraData: Set<String>,
    val strategy: (serverPage: ConfluencePage, content: PageContent) -> Boolean
) {
    HASH(emptySet(), { serverPage, content ->
        serverPage.pageProperty(HASH_PROPERTY)?.value != content.hash
    }),
    CONTENT(setOf("body.storage"), { serverPage, content ->
        serverPage.body?.storage?.value != content.body
    })
}

data class ServerPage(
    val id: String, val title: String, val parent: String, val labels: List<Label>, val attachments: List<Attachment>
)

class InvalidTenantException(page: String, expected: String?, actual: String?) :
    RuntimeException("Page $page must be in tenant \"${tenant(expected)}\" but actual is \"${tenant(actual)}\"")

private fun tenant(value: String?): String = value ?: "(no tenant)"
