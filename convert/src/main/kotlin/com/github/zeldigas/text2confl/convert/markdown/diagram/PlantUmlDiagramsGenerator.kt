package com.github.zeldigas.text2confl.convert.markdown.diagram

import com.github.zeldigas.text2confl.convert.markdown.PlantUmlDiagramsConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path

class PlantUmlDiagramsGenerator(
    val enabled: Boolean = true,
    val command: String = DEFAULT_COMMAND,
    override val defaultFileFormat: String = DEFAULT_FORMAT,
    val commandExecutor: CommandExecutor
) : DiagramGenerator {

    companion object {
        const val DEFAULT_COMMAND = "plantuml"
        val SUPPORTED_LANGS = setOf("plantuml", "puml")
        val SUPPORTED_FORMATS = setOf("svg", "png", "eps")
        const val DEFAULT_FORMAT = "png"
        private val logger = KotlinLogging.logger {  }
    }

    constructor(config: PlantUmlDiagramsConfiguration, commandExecutor: CommandExecutor = OsCommandExecutor()) : this(
        enabled = config.enabled,
        command = config.executable ?: DEFAULT_COMMAND,
        defaultFileFormat = config.defaultFormat,
        commandExecutor = commandExecutor
    )

    override val supportedFileFormats: Set<String>
        get() = SUPPORTED_FORMATS

    override fun generate(source: String, target: Path, attributes: Map<String, String>): ImageInfo {
        val executable = cmd(command) {
            flag("-pipe")
            flag("-t${resolveFormat(attributes)}")

            attributes["theme"]?.let { opt("-theme", it) }

            stdin(source)
            outputFile = target
        }

        val result = commandExecutor.execute(executable)

        if (result.status != 0) {
            throw DiagramGenerationFailedException("$command execution returned non-zero exit code: ${result.status}.\n${result.error}")
        }
        return ImageInfo()
    }

    override fun supports(lang: String): Boolean {
        return SUPPORTED_LANGS.contains(lang.lowercase())
    }

    override fun available(): Boolean {
        if (!enabled) return false
        if (!commandExecutor.commandAvailable(command)) return false

        val result = try {
            commandExecutor.execute(cmd(command) { flag("-version") })
        } catch (ex: Exception) {
            logger.debug(ex) { "Failed to execute command" }
            return false
        }
        return if (result.status == 0) {
            logger.info { "PlantUml version: ${result.output.lines().firstOrNull()}" }
            true
        } else {
            false
        }
    }
}