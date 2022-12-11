package com.github.zeldigas.text2confl.convert.markdown.diagram

import com.github.zeldigas.text2confl.convert.markdown.DiagramsConfiguration
import java.nio.file.Path

fun interface DiagramGeneratorsRegistry {

    companion object {
        val NOP: DiagramGeneratorsRegistry = DiagramGeneratorsRegistry { null }
    }

    fun findGenerator(lang: String): DiagramGenerator?

}

interface DiagramGenerator {

    fun generate(source: String, target: Path, attributes: Map<String, String> = emptyMap()): ImageInfo?

    fun name(baseName: String, attributes: Map<String, String> = emptyMap()): String

}

data class ImageInfo(
    val height: Int? = null,
    val width: Int? = null,
    val title: String? = null
) {
    val attributes: Map<String, String>
        get() = buildMap {
            height?.let { put("height", "$it") }
            width?.let { put("width", "$it") }
        }
}

class SimpleDiagramsRegistry(private val generators: Map<String, DiagramGenerator>) : DiagramGeneratorsRegistry {
    override fun findGenerator(lang: String): DiagramGenerator? = generators[lang]
}

fun loadAvailableGenerators(config: DiagramsConfiguration): DiagramGeneratorsRegistry {
    return SimpleDiagramsRegistry(buildMap {

    })
}


class DiagramGenerationFailedException(msg: String) : RuntimeException(msg)



