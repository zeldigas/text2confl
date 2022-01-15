package com.github.zeldigas.text2confl.convert

import com.github.zeldigas.text2confl.convert.confluence.LanguageMapper
import com.github.zeldigas.text2confl.convert.confluence.ReferenceProvider
import java.nio.file.Path

interface FileConverter {

    fun readHeader(file: Path, context: HeaderReadingContext): PageHeader

    fun convert(file: Path, context: ConvertingContext): PageContent

}

class ConversionFailedException(val file: Path, message: String, cause: Exception? = null) : RuntimeException(message, cause)

data class HeaderReadingContext(
    val titleTransformer: (Path, String) -> String
)

data class ConvertingContext(
    val referenceProvider: ReferenceProvider,
    val languageMapper: LanguageMapper,
    val targetSpace: String,
    val titleTransformer: (Path, String) -> String
)
