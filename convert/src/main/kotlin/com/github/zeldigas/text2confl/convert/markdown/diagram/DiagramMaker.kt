package com.github.zeldigas.text2confl.convert.markdown.diagram

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.markdown.DiagramsConfiguration
import com.github.zeldigas.text2confl.convert.toBase64
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

class DiagramMakersImpl(private val baseDir: Path, private val generators: List<DiagramGenerator>) : DiagramMakers {

    override fun find(lang: String): DiagramMaker? {
        val generator = generators.find { it.supports(lang) } ?: return null

        return DiagramMaker(baseDir, lang, generator)
    }
}

private val CACHE_FILE_MAPPER = jacksonObjectMapper()
private val logger = KotlinLogging.logger {  }

class DiagramMaker(
    internal val baseDir: Path,
    internal val lang: String,
    internal val generator: DiagramGenerator,
) {
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

fun createDiagramMakers(config: DiagramsConfiguration): DiagramMakers {
    return DiagramMakersImpl(
        config.diagramsBaseDir,
        loadAvailableGenerators(config)
    )
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