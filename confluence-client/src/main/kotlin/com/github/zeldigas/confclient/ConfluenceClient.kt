package com.github.zeldigas.confclient

import com.github.zeldigas.confclient.model.Attachment
import com.github.zeldigas.confclient.model.ConfluencePage
import com.github.zeldigas.confclient.model.PageAttachments
import com.github.zeldigas.confclient.model.Space

interface ConfluenceClient {

    suspend fun describeSpace(key:String, expansions: List<String>): Space

    suspend fun getPage(space: String, title: String,
                        status:List<String>? = null,
                        expansions: List<String> = emptyList()
    ): ConfluencePage

    suspend fun getPageOrNull(space: String, title: String,
                              status:List<String>? = null,
                              expansions: List<String> = emptyList()): ConfluencePage?

    suspend fun findPages(space: String?, title: String,
                        status:List<String>? = null,
                        expansions: List<String> = emptyList()
    ): List<ConfluencePage>

    suspend fun createPage(value: PageContentInput, updateParameters: PageUpdateOptions, expansions: List<String>? = null): ConfluencePage

    suspend fun updatePage(pageId: String, value: PageContentInput, updateParameters: PageUpdateOptions): ConfluencePage

    suspend fun setPageProperty(pageId: String, name: String, value: PagePropertyInput)

    suspend fun findChildPages(pageId: String, expansions: List<String>? = null): List<ConfluencePage>

    suspend fun deletePage(pageId: String)

    suspend fun deleteLabel(pageId: String, label: String)

    suspend fun addLabels(pageId: String, labels: List<String>)

    suspend fun addAttachments(pageId: String, pageAttachmentInput: List<PageAttachmentInput>): PageAttachments

    suspend fun updateAttachment(pageId: String, attachmentId: String, pageAttachmentInput: PageAttachmentInput): Attachment

    suspend fun deleteAttachment(attachmentId: String)

}

class PageNotFoundException : RuntimeException()

class TooManyPagesFound(val pages:List<ConfluencePage>) : RuntimeException()