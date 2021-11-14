package com.github.zeldigas.kustantaja.convert

import java.nio.file.Path

interface FileConverter {

    fun readHeader(file: Path): PageHeader

    fun convert(file: Path, context: ConvertingContext): PageContent

}

data class ConvertingContext(
    val referenceProvider: ReferenceProvider
)
