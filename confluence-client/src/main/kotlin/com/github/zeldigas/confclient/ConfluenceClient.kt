package com.github.zeldigas.confclient

import com.github.zeldigas.confclient.model.Attachment
import com.github.zeldigas.confclient.model.ConfluencePage
import com.github.zeldigas.confclient.model.PageAttachments
import com.github.zeldigas.confclient.model.Space
import io.ktor.http.*
import java.nio.file.Path

interface ConfluenceClient {

    val confluenceBaseUrl: Url
    val confluenceApiBaseUrl: Url

    suspend fun describeSpace(key: String, expansions: List<String>): Space

    suspend fun getPageById(id: String, expansions: Set<String>): ConfluencePage

    suspend fun getPage(
        space: String, title: String,
        status: List<String>? = null,
        expansions: Set<String> = emptySet()
    ): ConfluencePage

    suspend fun getPageOrNull(
        space: String, title: String,
        status: List<String>? = null,
        expansions: Set<String> = emptySet()
    ): ConfluencePage?

    suspend fun findPages(
        space: String?, title: String,
        status: List<String>? = null,
        expansions: Set<String> = emptySet()
    ): List<ConfluencePage>

    suspend fun createPage(
        value: PageContentInput,
        updateParameters: PageUpdateOptions,
        expansions: List<String>? = null
    ): ConfluencePage

    suspend fun updatePage(pageId: String, value: PageContentInput, updateParameters: PageUpdateOptions): ConfluencePage

    suspend fun changeParent(
        pageId: String,
        title: String,
        version: Int,
        newParentId: String,
        updateParameters: PageUpdateOptions
    ): ConfluencePage

    suspend fun setPageProperty(pageId: String, name: String, value: PagePropertyInput)

    suspend fun findChildPages(pageId: String, expansions: List<String>? = null): List<ConfluencePage>

    suspend fun deletePage(pageId: String)

    suspend fun deleteLabel(pageId: String, label: String)

    suspend fun addLabels(pageId: String, labels: List<String>)

    suspend fun fetchAllAttachments(pageAttachments: PageAttachments): List<Attachment>

    suspend fun addAttachments(pageId: String, pageAttachmentInput: List<PageAttachmentInput>): PageAttachments

    suspend fun updateAttachment(
        pageId: String,
        attachmentId: String,
        pageAttachmentInput: PageAttachmentInput
    ): Attachment

    suspend fun deleteAttachment(attachmentId: String)

    suspend fun downloadAttachment(attachment: Attachment, destination: Path)

}

class PageNotCreatedException(val title: String, val status: Int, val body: String?) :
    RuntimeException("Failed to create '$title' page: status=$status, body:\n$body")

class PageNotUpdatedException(val id: String, val status: Int, val body: String?) :
    RuntimeException("Failed to update '$id' page: status=$status, body:\n$body")

class PageNotFoundException : RuntimeException()

class TooManyPagesFound(val pages: List<ConfluencePage>) : RuntimeException()