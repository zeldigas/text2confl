package com.github.zeldigas.kustantaja.convert

import com.github.zeldigas.kustantaja.convert.markdown.MarkdownFileConverter
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

interface Converter {

    fun convertFile(file: Path): Page

    fun convertDir(dir: Path): List<Page>

}

fun universalConverter(): Converter {
    return UniversalConverter()
}

internal class UniversalConverter : Converter {

    private val converters: Map<String, FileConverter> = mapOf(
        "md" to MarkdownFileConverter()
    )

    override fun convertFile(file: Path): Page {
        val converter = converters[file.extension]
            ?: throw IllegalArgumentException("Unsupported extension: ${file.extension}")

        return Page(converter.convert(file, ConvertingContext(ReferenceProvider.nop())), file, emptyList())
    }

    override fun convertDir(dir: Path): List<Page> {
        val documents = scanDocuments(dir)

        return convertFilesInDirectory(dir, ConvertingContext(ReferenceProvider.fromDocuments(dir, documents)))
    }

    private fun scanDocuments(dir: Path) =
        dir.toFile().walk().filter { it.supported() }
            .map { it.toPath() to converters.getValue(it.extension.lowercase()).readHeader(it.toPath()) }
            .toMap()

    private fun convertFilesInDirectory(dir: Path, context: ConvertingContext): List<Page> {
        return dir.listDirectoryEntries().filter { it.supported() }.sorted()
            .map { file ->
                val content = convertSupported(file, context)
                val subdirectory = file.parent.resolve(file.nameWithoutExtension)
                val children = if (Files.exists(subdirectory) && Files.isDirectory(subdirectory)) {
                    convertFilesInDirectory(subdirectory, context)
                } else {
                    emptyList()
                }
                Page(content, file, children)
            }
    }

    private fun convertSupported(file: Path, context: ConvertingContext): PageContent {
        val converter =
            converters[file.extension] ?: throw IllegalArgumentException("Unsupported extension: ${file.extension}")
        return converter.convert(file, context)
    }

    private fun File.supported() = isFile && !name.startsWith("_") && extension.lowercase() in converters
    private fun Path.supported() = toFile().supported()
}