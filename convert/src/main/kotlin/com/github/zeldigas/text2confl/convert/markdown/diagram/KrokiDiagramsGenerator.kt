package com.github.zeldigas.text2confl.convert.markdown.diagram

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.github.zeldigas.text2confl.convert.markdown.KrokiDiagramsConfiguration
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.writeBytes

class KrokiDiagramsGenerator(
    private val enabled: Boolean,
    override val defaultFileFormat: String,
    private val server: URI
) : DiagramGenerator {

    constructor(config: KrokiDiagramsConfiguration) : this(
        config.enabled, config.defaultFormat, config.server
    )

    companion object {
        val DEFAULT_SERVER = URI.create("https://kroki.io")
        val SUPPORTED_DIAGRAMS = setOf(
            "puml", "plantuml", "mermaid", "ditaa", "blockdiag", "seqdiag", "actdiag",
            "nwdiag", "packetdiag", "rackdiag", "umlet", "graphviz", "dot", "erd", "svgbob", "nomnoml",
            "vega", "vegalite", "wavedrom", "bpmn", "bytefield", "excalidraw", "pikchr", "structurizr", "diagramsnet",
            "d2"
        )
        val SUPPORTED_FORMATS = setOf("png", "svg")
        const val DEFAULT_FORMAT = "png"
        private val internalNaming = mapOf(
            "puml" to "plantuml"
        )
        private val client: HttpClient by lazy {
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    jackson {
                        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    }
                }
                install(UserAgent) {
                    agent = "text2confl"
                }
                this.expectSuccess = false
            }
        }
    }

    override val supportedFileFormats: Set<String>
        get() = SUPPORTED_FORMATS

    override fun generate(source: String, target: Path, attributes: Map<String, String>): ImageInfo {
        val request = createRequest(source, attributes)
        runBlocking {
            val result = client.post(server.toURL()) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (result.status != HttpStatusCode.OK) {
                throw DiagramGenerationFailedException(result.body())
            }
            target.writeBytes(result.body())
        }
        return ImageInfo()
    }

    private fun createRequest(source: String, attributes: Map<String, String>) =
        KrokiDiagramRequest(
            source = source,
            type = attributes.getValue(DIAGRAM_FORMAT_ATTRIBUTE).let { internalNaming[it] ?: it },
            format = resolveFormat(attributes),
            options = attributes.asSequence()
                .filter { (k, _) -> k.startsWith("option_") }
                .map { (k, v) -> k.substringAfter("option_") to v }
                .toMap()
        )

    override fun supports(lang: String): Boolean {
        return SUPPORTED_DIAGRAMS.contains(lang)
    }

    override fun available(): Boolean {
        return enabled
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class KrokiDiagramRequest(
        @get:JsonProperty("diagram_source")
        val source: String,
        @get:JsonProperty("diagram_type")
        val type: String,
        @get:JsonProperty("output_format")
        val format: String,
        @get:JsonProperty("diagram_options")
        val options: Map<String, Any?> = emptyMap()
    )
}