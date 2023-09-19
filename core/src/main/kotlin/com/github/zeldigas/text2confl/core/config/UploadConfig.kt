package com.github.zeldigas.text2confl.core.config

import com.github.zeldigas.text2confl.core.upload.ChangeDetector

data class UploadConfig(
    val space: String,
    val removeOrphans: Cleanup,
    val uploadMessage: String,
    val notifyWatchers: Boolean,
    val modificationCheck: ChangeDetector,
    val tenant: String?
)

enum class Cleanup {
    None, Managed, All
}
