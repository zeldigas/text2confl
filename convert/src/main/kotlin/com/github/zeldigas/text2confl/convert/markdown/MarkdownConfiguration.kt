package com.github.zeldigas.text2confl.convert.markdown

import com.github.zeldigas.text2confl.convert.markdown.diagram.MermaidDiagramsGenerator
import com.github.zeldigas.text2confl.convert.markdown.diagram.PlantUmlDiagramsGenerator
import java.nio.file.Path
import kotlin.io.path.createTempDirectory

data class MarkdownConfiguration(
    val parseAnyMacro: Boolean = true,
    val supportedMacros: List<String> = emptyList(),
    val emoji: Boolean = true,
    val diagrams: DiagramsConfiguration = DiagramsConfiguration(createTempDirectory())
)

data class DiagramsConfiguration(
    val diagramsBaseDir: Path,
    val mermaid: MermaidDiagramsConfiguration = MermaidDiagramsConfiguration(),
    val plantuml: PlantUmlDiagramsConfiguration = PlantUmlDiagramsConfiguration()
)

interface DiagramsProviderConfiguration {
    val enabled: Boolean
    val executable: String?
}

data class MermaidDiagramsConfiguration(
    override val enabled: Boolean = true,
    val defaultFormat: String = MermaidDiagramsGenerator.DEFAULT_FORMAT,
    override val executable: String? = null,
    val configFile: String? = null,
    val cssFile: String? = null
) : DiagramsProviderConfiguration

data class PlantUmlDiagramsConfiguration(
    override val enabled: Boolean = true,
    override val executable: String? = null,
    val defaultFormat: String = PlantUmlDiagramsGenerator.DEFAULT_FORMAT
) : DiagramsProviderConfiguration
