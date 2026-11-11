package com.github.zeldigas.text2confl.core.config

import com.fasterxml.jackson.annotation.JsonValue
import com.github.zeldigas.text2confl.core.upload.ChangeDetector

data class UploadConfig(
    val space: String,
    val removeOrphans: Cleanup,
    val uploadMessage: String,
    val notifyWatchers: Boolean,
    val modificationCheck: ChangeDetector,
    val tenant: String?,
    val inlineComments: InlineComments
)

enum class Cleanup {
    None, Managed, All
}

enum class InlineComments(@get:JsonValue val code: String) {
    KEEP("keep"), DROP("drop"), KEEP_DRY("keep-dry")
}
