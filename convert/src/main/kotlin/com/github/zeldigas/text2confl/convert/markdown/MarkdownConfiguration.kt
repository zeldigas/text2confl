package com.github.zeldigas.text2confl.convert.markdown

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
    val executable: String?
}

data class MermaidDiagramsConfiguration(
    val defaultFormat: String = "png",
    override val executable: String? = null
) : DiagramsProviderConfiguration

data class PlantUmlDiagramsConfiguration(
    override val executable: String? = null
) : DiagramsProviderConfiguration
