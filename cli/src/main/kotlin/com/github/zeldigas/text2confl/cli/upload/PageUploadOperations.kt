package com.github.zeldigas.text2confl.cli.upload

import com.github.zeldigas.confclient.model.Attachment
import com.github.zeldigas.confclient.model.ConfluencePage
import com.github.zeldigas.confclient.model.Label
import com.github.zeldigas.text2confl.convert.Page
import com.github.zeldigas.text2confl.convert.PageContent

const val HASH_PROPERTY = "content-hash"
const val EDITOR_PROPERTY = "editor"

interface PageUploadOperations {

    suspend fun createOrUpdatePageContent(page: Page, space: String, parentPageId: String): ServerPage

    suspend fun updatePageLabels(serverPage: ServerPage, content: PageContent)

    suspend fun updatePageAttachments(serverPage: ServerPage, content: PageContent)

}

enum class ChangeDetector(
    val extraData: List<String>,
    val strategy: (serverPage: ConfluencePage, content: PageContent) -> Boolean
) {
    HASH(emptyList(), { serverPage, content ->
        serverPage.metadata?.properties?.get(HASH_PROPERTY)?.value != content.hash
    }),
    CONTENT(listOf("body.storage"), { serverPage, content ->
        serverPage.body?.storage?.value != content.body
    })
}

data class ServerPage(
    val id: String, val parent: String?, val labels: List<Label>, val attachments: List<Attachment>
)