package com.github.zeldigas.text2confl.convert.markdown

import com.github.zeldigas.text2confl.convert.markdown.diagram.KrokiDiagramsGenerator
import com.github.zeldigas.text2confl.convert.markdown.diagram.MermaidDiagramsGenerator
import com.github.zeldigas.text2confl.convert.markdown.diagram.PlantUmlDiagramsGenerator
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.createTempDirectory

data class MarkdownConfiguration(
    val parseAnyMacro: Boolean = true,
    val supportedMacros: List<String> = emptyList(),
    val emoji: Boolean = true,
    val diagrams: DiagramsConfiguration = DiagramsConfiguration(createTempDirectory()),
    val tables: TablesConfiguration = TablesConfiguration(),
    val autoLinks: Boolean = true,
    val typography: TypographyConfiguration = TypographyConfiguration()
)

data class DiagramsConfiguration(
    val diagramsBaseDir: Path,
    val mermaid: MermaidDiagramsConfiguration = MermaidDiagramsConfiguration(),
    val plantuml: PlantUmlDiagramsConfiguration = PlantUmlDiagramsConfiguration(),
    val kroki: KrokiDiagramsConfiguration = KrokiDiagramsConfiguration()
)

interface DiagramsProviderConfiguration {
    val enabled: Boolean
}

data class MermaidDiagramsConfiguration(
    override val enabled: Boolean = true,
    val defaultFormat: String = MermaidDiagramsGenerator.DEFAULT_FORMAT,
    val executable: String? = null,
    val configFile: String? = null,
    val cssFile: String? = null,
    val puppeeterConfig: String? = null,
) : DiagramsProviderConfiguration

data class PlantUmlDiagramsConfiguration(
    override val enabled: Boolean = true,
    val executable: String? = null,
    val defaultFormat: String = PlantUmlDiagramsGenerator.DEFAULT_FORMAT
) : DiagramsProviderConfiguration

data class KrokiDiagramsConfiguration(
    override val enabled: Boolean = true,
    val server: URI = KrokiDiagramsGenerator.DEFAULT_SERVER,
    val defaultFormat: String = KrokiDiagramsGenerator.DEFAULT_FORMAT
): DiagramsProviderConfiguration

data class TablesConfiguration(
    val columnSpans: Boolean = true,
    val discardExtraColumns: Boolean = true,
    val appendMissingColumns: Boolean = true,
    val headerSeparatorColumnMatch: Boolean = true
)

data class TypographyConfiguration(
    val quotes: Boolean = false,
    val smarts: Boolean = true
) {
    val enabled: Boolean
        get() = quotes || smarts
}