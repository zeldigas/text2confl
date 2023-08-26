package com.github.zeldigas.text2confl.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.zeldigas.text2confl.cli.config.createConversionConfig
import com.github.zeldigas.text2confl.cli.config.readDirectoryConfig
import com.github.zeldigas.text2confl.convert.Converter
import com.github.zeldigas.text2confl.convert.EditorVersion
import com.github.zeldigas.text2confl.convert.Page
import io.ktor.http.*
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

class Convert : CliktCommand(name = "convert", help = "Converts source files to confluence markup"), WithConversionOptions {

    private val docs: File by docsLocation()
    override val spaceKey: String? by confluenceSpace()
    private val useTitleAsOutFile by option("--use-title").flag("--no-use-title")
        .help("If title of document should be used in resulting filename instead of plain original filenames")
    private val copyAttachments by option("--copy-attachments").flag("--no-copy-attachments")
        .help("Copy attachments to destination directory")
    private val out: File by option("--out").file(canBeFile = false, canBeDir = true, mustExist = false)
        .default(File("out"))
        .help("Output directory where converted contents should be generated")
    private val validate by option("--validate").flag("--no-validate", default = true)
        .help("Perform validation of converted files for correctness")
    override val editorVersion: EditorVersion? by editorVersion()

    private val serviceProvider: ServiceProvider by requireObject()

    override fun run() {
        val directoryConfig = readDirectoryConfig(docs.toPath())
        val conversionConfig = createConversionConfig(directoryConfig, editorVersion, directoryConfig.server?.let { Url(it) })
        val space = spaceKey ?: directoryConfig.space ?: "AAA"
        val converter = serviceProvider.createConverter(space, conversionConfig)
        try {
            val result = tryConvert(converter)
            if (validate) {
                val validator = serviceProvider.createContentValidator()
                validator.validate(result)
            }
        } catch (ex: Exception) {
            tryHandleException(ex)
        }
    }

    private fun tryConvert(converter: Converter): List<Page> {
        val result = if (docs.isFile) {
            listOf(converter.convertFile(docs.toPath()))
        } else {
            converter.convertDir(docs.toPath())
        }
        save(result, out)
        return result
    }

    private fun save(result: List<Page>, out: File) {
        if (!out.exists() && !out.mkdirs()) {
            throw IllegalStateException("Failed to create required directories")
        }

        val outPath = out.toPath()

        for (page in result) {
            val resultName = if (useTitleAsOutFile) sanitizeTitle(page) else page.source.fileName.nameWithoutExtension
            savePage(outPath, resultName, page)
            if (page.children.isNotEmpty()) {
                save(page.children, out.resolve(resultName))
            }
        }
    }

    private fun savePage(outPath: Path, resultName: String, page: Page) {
        (outPath / "${resultName}.html").writeText(page.content.body)
        if (copyAttachments && page.content.attachments.isNotEmpty()) {
            val attachmentDir = (outPath / "${resultName}_attachments").createDirectories()
            page.content.attachments.forEach{it.resourceLocation.copyTo(attachmentDir / it.attachmentName, overwrite = true)}
        }
    }
}

fun sanitizeTitle(page: Page) = sanitizeTitle(page.content.header.title)
fun sanitizeTitle(title: String) = title.replace("""[^a-zA-Z0-9._ -]+""".toRegex(), "_")