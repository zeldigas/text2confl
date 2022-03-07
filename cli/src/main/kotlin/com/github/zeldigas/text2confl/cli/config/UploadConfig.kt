package com.github.zeldigas.text2confl.cli.config

import com.github.zeldigas.text2confl.cli.upload.ChangeDetector

data class UploadConfig(
    val space: String,
    val removeOrphans: Cleanup,
    val uploadMessage: String,
    val notifyWatchers: Boolean,
    val modificationCheck: ChangeDetector
)

enum class Cleanup {
    None, Managed, All
}
