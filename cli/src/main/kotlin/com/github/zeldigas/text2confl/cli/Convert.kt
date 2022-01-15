package com.github.zeldigas.text2confl.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.zeldigas.text2confl.convert.Converter
import com.github.zeldigas.text2confl.convert.Page
import com.github.zeldigas.text2confl.convert.confluence.LanguageMapper
import com.github.zeldigas.text2confl.convert.universalConverter
import java.io.File
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.writeText

class Convert : CliktCommand(name = "convert", help = "Converts source files to confluence markup") {

    private val docs: File by option("--docs").file(canBeFile = true, canBeDir = true).required()
        .help("File or directory with files to convert")
    private val space: String by option("--space")
        .help("Space key to use if it is required in output format").default("AAA")
    private val useTitleAsOutFile by option("--use-title").flag("--no-use-title")
        .help("If title of document should be used in resulting filename instead of plain original filenames")
    private val copyAttachments by option("--copy-attachments").flag("--no-copy-attachments")
        .help("Copy attachments to destination directory")
    private val out: File by option("--out").file(canBeFile = false, canBeDir = true, mustExist = false)
        .default(File("out"))
        .help("Output directory where converted contents should be generated")


    override fun run() {
        val converter = universalConverter(space, LanguageMapper.forCloud())
        try {
            tryConvert(converter)
        } catch (ex: Exception) {
            tryHandleException(ex)
        }
    }

    private fun tryConvert(converter: Converter) {
        val result = if (docs.isFile) {
            listOf(converter.convertFile(docs.toPath()))
        } else {
            converter.convertDir(docs.toPath())
        }
        save(result, out)
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
    }

    private fun sanitizeTitle(page: Page) = page.content.header.title //todo sanitize title
}