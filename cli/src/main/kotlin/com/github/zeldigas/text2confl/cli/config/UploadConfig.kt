package com.github.zeldigas.text2confl.cli.config

import com.github.zeldigas.text2confl.cli.ChangeDetector

data class UploadConfig(
    val space: String,
    val removeOrphans: Boolean,
    val uploadMessage: String,
    val notifyWatchers: Boolean,
    val modificationCheck: ChangeDetector
)
