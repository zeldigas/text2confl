package com.github.zeldigas.text2confl.convert.markdown.diagram

import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.markdown.DiagramsConfiguration
import com.github.zeldigas.text2confl.convert.toBase64
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.div
import kotlin.io.path.exists

fun interface DiagramMakers {
    companion object {
        val NOP: DiagramMakers = DiagramMakers { null }
    }

    fun find(lang: String): DiagramMaker?
}

class DiagramMakersImpl(val baseDir: Path, val generators: List<DiagramGenerator>) : DiagramMakers {

    override fun find(lang: String): DiagramMaker? {
        val generator = generators.find { it.supports(lang) } ?: return null

        return DiagramMaker(generator, baseDir)
    }
}

class DiagramMaker(
    private val generator: DiagramGenerator,
    private val baseDir: Path,
) {
    fun toDiagram(script: String, attributes: Map<String, String>, pathPrefix: Path?): Pair<Attachment, ImageInfo> {
        val name = generator.name(baseName(script, attributes), attributes)

        val generatedFileLocation = if (pathPrefix == null) baseDir / name else baseDir / pathPrefix / name

        if (!generatedFileLocation.parent.exists()){
            Files.createDirectories(generatedFileLocation.parent)
        }

        val result = generator.generate(script, generatedFileLocation, attributes)

        return Attachment(name, "_generated_diagram_${name}", generatedFileLocation) to result
    }

    private val String.contentHash: String
        get() {
            val sha256 = MessageDigest.getInstance("SHA-256")
            sha256.update(this.toByteArray())
            return toBase64(sha256.digest())
        }

    private fun baseName(script: String, attributes: Map<String, String>): String {
        return attributes["target"] ?: script.contentHash
    }
}

fun createDiagramMakers(config: DiagramsConfiguration): DiagramMakers {
    return DiagramMakersImpl(
        config.diagramsBaseDir,
        listOf(
            MermaidDiagramsGenerator(config.mermaid.defaultFormat, config.mermaid.executable ?: "mmdc")
        )
    )
}