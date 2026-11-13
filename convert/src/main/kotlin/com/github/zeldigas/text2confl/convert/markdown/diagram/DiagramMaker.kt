package com.github.zeldigas.text2confl.convert.markdown.diagram

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.markdown.DiagramMacroConfiguration
import com.github.zeldigas.text2confl.convert.markdown.DiagramsConfiguration
import com.github.zeldigas.text2confl.convert.toBase64
import com.samskivert.mustache.Escapers
import com.samskivert.mustache.Mustache
import com.samskivert.mustache.MustacheException
import com.samskivert.mustache.Template
import io.github.oshai.kotlinlogging.KotlinLogging
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

class DiagramMakersImpl(
    private val baseDir: Path,
    private val generators: List<DiagramGenerator>,
    private val inlineTemplates: Map<String, Template> = emptyMap(),
) : DiagramMakers {

    override fun find(lang: String): DiagramMaker? {
        inlineTemplates[lang]?.let { return InlineDiagramMaker(lang, it) }

        val generator = generators.find { it.supports(lang) } ?: return null

        return ImageDiagramMaker(baseDir, lang, generator)
    }
}

/**
 * Turns diagram code block into Confluence content
 */
sealed interface DiagramMaker {
    val lang: String
}

private val CACHE_FILE_MAPPER = jacksonObjectMapper()
private val logger = KotlinLogging.logger {  }

/**
 * Generates diagram image that is uploaded as page attachment
 */
class ImageDiagramMaker(
    internal val baseDir: Path,
    override val lang: String,
    internal val generator: DiagramGenerator,
) : DiagramMaker {
    fun toDiagram(script: String, attributes: Map<String, String>, pathPrefix: Path?): Pair<Attachment, ImageInfo> {
        val name = generator.name(baseName(script, attributes), attributes + mapOf(DIAGRAM_FORMAT_ATTRIBUTE to lang))

        val generatedFileLocation = if (pathPrefix == null) baseDir / name else baseDir / pathPrefix / name

        if (!generatedFileLocation.parent.exists()) {
            Files.createDirectories(generatedFileLocation.parent)
        }
        val conversionOptions = generator.conversionOptions(attributes + mapOf("lang" to lang))
        val cacheState = checkDiagramCache(generatedFileLocation, script, conversionOptions)
        return if (cacheState is Cached) {
            logger.debug { "Found cached diagram $generatedFileLocation, reusing it" }
            generatedFileLocation.attachment(name) to cacheState.imageInfo
        } else {
            logger.debug { "Cache state for ${generatedFileLocation}: $cacheState, generating diagram" }
            val result = generator.generate(script, generatedFileLocation, conversionOptions)
            saveCache(result, generatedFileLocation, conversionOptions, script)

            generatedFileLocation.attachment(name) to result
        }
    }

    private fun checkDiagramCache(
        generatedFileLocation: Path,
        script: String,
        conversionOptions: Map<String, String>
    ):CacheState {
        val cacheFile = cacheFile(generatedFileLocation)
        if (cacheFile.exists()) {
            val cacheData = try {
                CACHE_FILE_MAPPER.readValue<DiagramCacheInfo>(cacheFile.toFile())
            } catch (_: Exception) {
                return Missing()
            }
            val checksum = script.contentHash
            return if (checksum != cacheData.checksum || conversionOptions != cacheData.conversionOptions) {
                NotMatch(checksum)
            } else {
                Cached(cacheData.imageInfo)
            }
        } else {
            return Missing()
        }
    }

    private fun saveCache(
        result: ImageInfo,
        generatedFileLocation: Path,
        conversionOptions: Map<String, String>,
        script: String
    ) {
        val cacheFile = cacheFile(generatedFileLocation)
        CACHE_FILE_MAPPER.writeValue(cacheFile.toFile(), DiagramCacheInfo(
            result, conversionOptions, script.contentHash
        ))
    }

    private fun cacheFile(generatedFileLocation: Path): Path =
        generatedFileLocation.parent / "${generatedFileLocation.fileName}.cache"

    private fun Path.attachment(
        name: String
    ): Attachment = Attachment(name, "_generated_diagram_${name}", this)

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

/**
 * Renders diagram code block as inline content (e.g. Confluence macro) using Mustache template. Available in template:
 * * `{{lang}}` - code block language
 * * `{{source}}` - diagram source, XML-escaped; `{{{source}}}` - as is
 * * `{{#cdata}}...{{/cdata}}` - wraps content in CDATA section, splitting `]]>` occurrences so that section is not
 * terminated early. Use with unescaped source: `{{#cdata}}{{{source}}}{{/cdata}}`
 */
class InlineDiagramMaker(
    override val lang: String,
    internal val template: Template,
) : DiagramMaker {
    fun toContent(script: String): String =
        template.execute(mapOf("lang" to lang, "source" to script, "cdata" to CDATA))

    companion object {
        private val COMPILER: Mustache.Compiler = Mustache.compiler().withEscaper(
            Escapers.simple(
                arrayOf("&", "&amp;"),
                arrayOf("<", "&lt;"),
                arrayOf(">", "&gt;"),
                arrayOf("\"", "&quot;"),
                arrayOf("'", "&#39;"),
            )
        )

        private val CDATA = Mustache.Lambda { fragment, out ->
            out.write("<![CDATA[")
            out.write(fragment.execute().replace("]]>", "]]]]><![CDATA[>"))
            out.write("]]>")
        }

        fun compile(template: String): Template = COMPILER.compile(template)
    }
}

fun createDiagramMakers(config: DiagramsConfiguration): DiagramMakers {
    return DiagramMakersImpl(
        config.diagramsBaseDir,
        loadAvailableGenerators(config),
        inlineTemplates(config.macros)
    )
}

internal fun inlineTemplates(macros: Map<String, DiagramMacroConfiguration>): Map<String, Template> {
    val result = mutableMapOf<String, Template>()
    val owners = mutableMapOf<String, String>()
    for ((name, macro) in macros) {
        val template = try {
            InlineDiagramMaker.compile(macro.template)
        } catch (e: MustacheException) {
            throw IllegalArgumentException("Invalid template of diagram macro '$name': ${e.message}", e)
        }
        for (format in macro.formats) {
            owners[format]?.let {
                throw IllegalArgumentException("Diagram format '$format' is configured in both '$it' and '$name' macros")
            }
            owners[format] = name
            result[format] = template
        }
    }
    return result
}

sealed class CacheState

data class Cached(val imageInfo: ImageInfo) : CacheState() {}
data class NotMatch(val newChecksum: String) : CacheState() {}
class Missing() : CacheState() {
    override fun toString(): String {
        return "Missing"
    }
}

data class DiagramCacheInfo(
    val imageInfo: ImageInfo,
    val conversionOptions: Map<String, Any>,
    val checksum: String,
    ) : CacheState()