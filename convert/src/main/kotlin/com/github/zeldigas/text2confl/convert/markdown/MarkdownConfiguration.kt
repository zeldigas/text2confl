package com.github.zeldigas.text2confl.convert.markdown

data class MarkdownConfiguration(
    val parseAnyMacro:Boolean = true,
    val supportedMacros:List<String> = emptyList()
)
