package com.github.zeldigas.text2confl.convert.asciidoc

data class AsciidoctorConfiguration(
    val libsToLoad: List<String> = emptyList(),
    val loadBundledMacros: Boolean = true,
    val attributes: Map<String, Any?> = emptyMap()
)
