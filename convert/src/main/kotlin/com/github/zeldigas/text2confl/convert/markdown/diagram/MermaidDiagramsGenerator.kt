package com.github.zeldigas.text2confl.convert.markdown.diagram

import com.github.zeldigas.text2confl.convert.markdown.MermaidDiagramsConfiguration
import mu.KotlinLogging
import java.nio.file.Path

class MermaidDiagramsGenerator(
    private val enabled: Boolean = true,
    private val defaultFormat: String = DEFAULT_FORMAT,
    private val command: String = DEFAULT_COMMAND,
    private val commandExecutor: CommandExecutor = OsCommandExecutor(),
    private val configFile: String? = null,
    private val cssFile: String? = null
) : DiagramGenerator {
    companion object {
        const val DEFAULT_COMMAND = "mmdc"
        const val DEFAULT_FORMAT = "png"
        private val SUPPORTED_LANGUAGES = setOf("mermaid")
        private val SUPPORTED_FORMATS = setOf("png", "svg")

        private val log = KotlinLogging.logger {}
    }

    constructor(config: MermaidDiagramsConfiguration, commandExecutor: CommandExecutor = OsCommandExecutor()) : this(
        config.enabled,
        config.defaultFormat,
        config.executable ?: DEFAULT_COMMAND,
        configFile = config.configFile,
        cssFile =  config.cssFile,
        commandExecutor = commandExecutor
    )

    override fun supports(lang: String): Boolean = SUPPORTED_LANGUAGES.contains(lang)

    override fun generate(source: String, target: Path, attributes: Map<String, String>): ImageInfo {
        val executable = cmd(command) {
            opt("--output", target.toString())
            opt("--outputFormat", resolveFormat(attributes))
            configFile?.let { opt("--configFile", it) }
            cssFile?.let { opt("--cssFile", it) }
            flag("--quiet")

            stdin(source)
        }

        val result = commandExecutor.execute(executable)

        if (result.status != 0) {
            throw DiagramGenerationFailedException("$command execution returned non-zero exit code: ${result.status}.\n${result.output}")
        }
        return ImageInfo()
    }

    override fun name(baseName: String, attributes: Map<String, String>): String {
        return "$baseName.${resolveFormat(attributes)}"
    }

    private fun resolveFormat(attributes: Map<String, String>): String {
        val resultingFormat = attributes["format"] ?: defaultFormat

        if (resultingFormat !in SUPPORTED_FORMATS) {
            throw IllegalStateException("Requested format for diagram - $resultingFormat, but only following formats are supported: $SUPPORTED_FORMATS")
        }

        return resultingFormat
    }

    override fun available(): Boolean {
        if (!enabled) return false;
        if (!commandExecutor.commandAvailable(command)) return false

        val result = try {
            commandExecutor.execute(cmd(command) { flag("-V") })
        } catch (ex: Exception) {
            log.debug(ex) { "Failed to execute command" }
            return false
        }
        return if (result.status == 0) {
            log.info { "Mermaid version: ${result.output}" }
            true
        } else {
            false
        }
    }
}