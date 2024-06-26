package com.github.zeldigas.text2confl.convert

import com.github.zeldigas.text2confl.convert.asciidoc.AsciidocFileConverter
import com.github.zeldigas.text2confl.convert.asciidoc.AsciidoctorConfiguration
import com.github.zeldigas.text2confl.convert.confluence.LanguageMapper
import com.github.zeldigas.text2confl.convert.confluence.ReferenceProvider
import com.github.zeldigas.text2confl.convert.markdown.MarkdownConfiguration
import com.github.zeldigas.text2confl.convert.markdown.MarkdownFileConverter
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.relativeTo

interface Converter {

    fun convertFile(file: Path): Page

    fun convertDir(dir: Path): List<Page>

}

open class ConversionException(message: String, e: Throwable? = null) : RuntimeException(message, e)

class FileDoesNotExistException(val file: Path) : ConversionException("File does not exist: $file")
class DuplicateTitlesException(val duplicates: List<String>, message: String) : ConversionException(message)

const val DEFAULT_AUTOGEN_BANNER =
    "Edit <a href=\"__doc-root____file__\">source file</a> instead of changing page in Confluence. " +
            "<span style=\"color: rgb(122,134,154); font-size: small;\">Page was generated from source with <a href=\"https://github.com/zeldigas/text2confl\">text2confl</a>.</span>"

enum class EditorVersion {
    V1, V2
}

data class ConversionParameters(
    val languageMapper: LanguageMapper,
    val titleConverter: (Path, String) -> String = { _, title -> title },
    val addAutogeneratedNote: Boolean = true,
    val docRootLocation: String = "",
    val noteText: String = DEFAULT_AUTOGEN_BANNER,
    val markdownConfiguration: MarkdownConfiguration = MarkdownConfiguration(),
    val asciidoctorConfiguration: AsciidoctorConfiguration = AsciidoctorConfiguration(),
    val editorVersion: EditorVersion
)

fun universalConverter(
    space: String,
    parameters: ConversionParameters
): Converter {
    return UniversalConverter(
        space, parameters, mapOf(
            "md" to MarkdownFileConverter(parameters.markdownConfiguration),
            "adoc" to AsciidocFileConverter(parameters.asciidoctorConfiguration)
        ), FileNameBasedDetector
    )
}

internal class UniversalConverter(
    val space: String,
    val conversionParameters: ConversionParameters,
    val converters: Map<String, FileConverter>,
    val pagesDetector: PagesDetector,
) : Converter {

    override fun convertFile(file: Path): Page {
        if (!file.exists()) {
            throw FileDoesNotExistException(file)
        }

        return Page(
            performConversion(file, ConvertingContext(ReferenceProvider.singleFile(), conversionParameters, space)),
            file,
            emptyList()
        )
    }

    override fun convertDir(dir: Path): List<Page> {
        val documents = scanDocuments(dir)

        checkForDuplicates(dir, documents)

        return convertFilesInDirectory(
            dir,
            ConvertingContext(ReferenceProvider.fromDocuments(dir, documents), conversionParameters, space)
        )
    }

    private fun checkForDuplicates(base: Path, documents: Map<Path, PageHeader>) {
        val duplicates = documents.entries
            .groupBy { (_, v) -> v.title }
            .entries.asSequence()
            .filter { (_, v) -> v.size > 1 }
            .map { (title, v) -> title to v.map { it.key.relativeTo(base) }.sorted() }
            .map { (title, paths) -> "\"$title\": ${paths.joinToString(", ")}" }
            .toList()
        if (duplicates.isNotEmpty()) {
            throw DuplicateTitlesException(
                duplicates,
                "Files with duplicate titles detected. Confluence has flat structure and every published page must have unique title.\n${
                    duplicates.joinToString("\n")
                }"
            )
        }
    }

    private fun scanDocuments(dir: Path): Map<Path, PageHeader> {
        val headers = mutableMapOf<Path, PageHeader>()
        val context = HeaderReadingContext(conversionParameters.titleConverter)
        pagesDetector.scanDirectoryRecursively(dir,
            filter = { it.supported() },
            converter = { file ->
                headers[file] = converterFor(file).readHeader(file, context)
            },
            assembler = { _, _, _ -> }
        )
        return headers
    }

    private fun convertFilesInDirectory(dir: Path, context: ConvertingContext): List<Page> =
        pagesDetector.scanDirectoryRecursively(dir,
            filter = { it.supported() },
            converter = { file -> performConversion(file, context) },
            assembler = { file, content, children -> Page(content, file, children) }
        )

    private fun converterFor(file: Path) =
        converters[file.extension.lowercase()]
            ?: throw IllegalArgumentException("Unsupported extension: ${file.extension}")

    private fun performConversion(file: Path, context: ConvertingContext): PageContent {
        val convert = converterFor(file)
        return try {
            convert.convert(file, context)
        } catch(e: Exception) {
            throw ConversionException("Failed to convert $file: ${e.message}", e)
        }
    }

    private fun File.supported() = isFile && !name.startsWith("_") && extension.lowercase() in converters
    private fun Path.supported() = toFile().supported()
}