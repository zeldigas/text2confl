package com.github.zeldigas.text2confl.convert.markdown.diagram

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.github.zeldigas.text2confl.convert.markdown.DiagramsConfiguration
import java.nio.file.Path

const val DIAGRAM_FORMAT_ATTRIBUTE = "lang"

interface DiagramGenerator {

    fun conversionOptions(attributes: Map<String, String>): Map<String, String> = attributes

    fun generate(source: String, target: Path, conversionOptions: Map<String, String>): ImageInfo

    fun name(baseName: String, attributes: Map<String, String> = emptyMap()): String =
        "$baseName.${resolveFormat(attributes)}"

    fun supports(lang: String): Boolean

    fun available(): Boolean

    val defaultFileFormat: String

    val supportedFileFormats: Set<String>
}

fun DiagramGenerator.resolveFormat(attributes: Map<String, String>): String {
    val resultingFormat = attributes["format"] ?: defaultFileFormat

    if (resultingFormat !in supportedFileFormats) {
        throw IllegalStateException("Requested format for diagram - $resultingFormat, but only following formats are supported: $supportedFileFormats")
    }

    return resultingFormat
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ImageInfo(
    val height: Int? = null,
    val width: Int? = null,
    val title: String? = null
) {
    @get:JsonIgnore
    val attributes: Map<String, String>
        get() = buildMap {
            height?.let { put("height", "$it") }
            width?.let { put("width", "$it") }
        }
}

fun loadAvailableGenerators(
    config: DiagramsConfiguration,
    commandExecutor: CommandExecutor = OsCommandExecutor()
): List<DiagramGenerator> {
    val candidates: List<DiagramGenerator> = listOf(
        PlantUmlDiagramsGenerator(config.plantuml, commandExecutor),
        MermaidDiagramsGenerator(config.mermaid, commandExecutor),
        KrokiDiagramsGenerator(config.kroki)
    )
    return candidates.filter { it.available() }
}


class DiagramGenerationFailedException(msg: String) : RuntimeException(msg)



