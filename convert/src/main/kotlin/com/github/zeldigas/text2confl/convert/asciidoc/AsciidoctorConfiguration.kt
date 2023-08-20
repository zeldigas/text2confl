package com.github.zeldigas.text2confl.convert.asciidoc

import java.nio.file.Files
import java.nio.file.Path

data class AsciidoctorConfiguration(
    val libsToLoad: List<String> = emptyList(),
    val loadBundledMacros: Boolean = true,
    val attributes: Map<String, Any?> = emptyMap(),
    val workdir: Path = Files.createTempDirectory("asciidoc"),
)
