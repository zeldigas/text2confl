package com.github.zeldigas.text2confl.convert.markdown.diagram

import java.nio.file.Path

class MermaidDiagramsGenerator(
    val defaultFormat: String,
    val command: String,
) : DiagramGenerator {
    companion object {
        val SUPPORTED_LANGUAGES = setOf("mermaid")
        val SUPPORTED_FORMATS = setOf("png", "svg")
    }

    override fun supports(lang: String): Boolean = SUPPORTED_LANGUAGES.contains(lang)

    override fun generate(source: String, target: Path, attributes: Map<String, String>): ImageInfo {
        val process = ProcessBuilder(
            command,
            "--output", target.toString(),
            "--outputFormat", resolveFormat(attributes),
            "--quiet"
        ).start()
        process.outputStream.apply {
            this.bufferedWriter().append(source).close()
        }
        val result = process.waitFor()
        val output = process.inputStream.bufferedReader().readText()
        if (result != 0) {
            throw DiagramGenerationFailedException("$command execution returned non-zero exit code: $result.\n$output")
        }
        return ImageInfo()
    }

    override fun name(baseName: String, attributes: Map<String, String>): String {
        return "$baseName.${resolveFormat(attributes)}"
    }

    private fun resolveFormat(attributes: Map<String, String>): String {
        val resultingFormat = attributes["format"] ?: defaultFormat

        if (resultingFormat !in SUPPORTED_FORMATS) {
            throw IllegalStateException("Requested format for diagram - $resultingFormat, but only following formats are supported: $SUPPORTED_FORMATS")
        }

        return resultingFormat
    }
}