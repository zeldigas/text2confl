package com.github.zeldigas.text2confl.cli.config

import com.github.zeldigas.text2confl.convert.confluence.LanguageMapper
import java.nio.file.Path

enum class EditorVersion {
    V1, V2
}

data class ConverterConfig(
    val titlePrefix: String,
    val titlePostfix: String,
    val editorVersion: EditorVersion
) {
    val languageMapper: LanguageMapper
        get () = when(editorVersion) {
            EditorVersion.V1 -> LanguageMapper.forServer()
            EditorVersion.V2 -> LanguageMapper.forCloud()
        }

    val titleConverter: (Path, String) -> String
        get() = { _, title -> "$titlePrefix${title}${titlePostfix}"}
}
