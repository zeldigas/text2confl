package com.github.zeldigas.kustantaja.cli.config

import com.github.zeldigas.kustantaja.cli.ChangeDetector

data class UploadConfig(
    val space: String,
    val removeOrphans: Boolean,
    val uploadMessage: String,
    val notifyWatchers: Boolean,
    val modificationCheck: ChangeDetector
)
