package com.github.zeldigas.text2confl.convert.markdown.diagram

import java.nio.file.Path

class MermaidDiagramsGenerator(
    val defaultFormat: String,
    val command: String,
) : DiagramGenerator {

    override fun generate(source: String, target: Path, attributes: Map<String, String>): ImageInfo? {
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
        if (result != 0) {
            throw DiagramGenerationFailedException("mmdc execution returned non-zero exit code: $result")
        }
        return null
    }

    override fun name(baseName: String, attributes: Map<String, String>): String {
        return "$baseName.${resolveFormat(attributes)}"
    }

    private fun resolveFormat(attributes: Map<String, String>) = attributes["format"] ?: defaultFormat
}