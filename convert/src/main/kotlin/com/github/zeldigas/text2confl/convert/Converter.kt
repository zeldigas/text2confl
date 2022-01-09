package com.github.zeldigas.text2confl.convert

import com.github.zeldigas.text2confl.convert.confluence.LanguageMapper
import com.github.zeldigas.text2confl.convert.confluence.ReferenceProvider
import com.github.zeldigas.text2confl.convert.markdown.MarkdownFileConverter
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

fun universalConverter(
    space: String,
    languageMapper: LanguageMapper,
    titleConverter: (Path, String) -> String = { _, title -> title },
): Converter {
    return UniversalConverter(space, languageMapper, titleConverter)
}

internal class UniversalConverter(
    private val space: String,
    private val languageMapper: LanguageMapper,
    private val titleConverter: (Path, String) -> String,
) : Converter {

    private val converters: Map<String, FileConverter> = mapOf(
        "md" to MarkdownFileConverter()
    )

    override fun convertFile(file: Path): Page {
        val converter = converters[file.extension]
            ?: throw IllegalArgumentException("Unsupported extension: ${file.extension}")

        return Page(
            converter.convert(file, ConvertingContext(ReferenceProvider.nop(), languageMapper, "", titleConverter)),
            file,
            emptyList()
        )
    }

    override fun convertDir(dir: Path): List<Page> {
        val documents = scanDocuments(dir)

        return convertFilesInDirectory(
            dir,
            ConvertingContext(ReferenceProvider.fromDocuments(dir, documents), languageMapper, space, titleConverter)
        )
    }

    private fun scanDocuments(dir: Path) =
        dir.toFile().walk().filter { it.supported() }
            .map { it.toPath() to converters.getValue(it.extension.lowercase()).readHeader(it.toPath(), HeaderReadingContext(titleConverter)) }
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