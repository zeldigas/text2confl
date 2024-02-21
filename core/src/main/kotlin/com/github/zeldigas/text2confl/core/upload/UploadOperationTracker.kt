package com.github.zeldigas.text2confl.core.upload

import com.github.zeldigas.text2confl.model.ConfluencePage

interface UploadOperationTracker {
    fun uploadsCompleted()
    fun pagesDeleted(root: ConfluencePage, allDeletedPages: List<ConfluencePage>)
    fun pageUpdated(
        pageResult: PageOperationResult,
        labelUpdate: LabelsUpdateResult,
        attachmentsUpdated: AttachmentsUpdateResult
    )
}

val NOP: UploadOperationTracker = object : UploadOperationTracker {

    override fun uploadsCompleted() = Unit

    override fun pagesDeleted(root: ConfluencePage, allDeletedPages: List<ConfluencePage>) = Unit

    override fun pageUpdated(
        pageResult: PageOperationResult,
        labelUpdate: LabelsUpdateResult,
        attachmentsUpdated: AttachmentsUpdateResult
    ) = Unit
}