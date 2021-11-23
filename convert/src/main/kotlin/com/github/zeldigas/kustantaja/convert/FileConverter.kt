package com.github.zeldigas.kustantaja.convert

import com.github.zeldigas.kustantaja.convert.confluence.LanguageMapper
import com.github.zeldigas.kustantaja.convert.confluence.ReferenceProvider
import java.nio.file.Path

interface FileConverter {

    fun readHeader(file: Path): PageHeader

    fun convert(file: Path, context: ConvertingContext): PageContent

}

data class ConvertingContext(
    val referenceProvider: ReferenceProvider,
    val languageMapper: LanguageMapper
)
