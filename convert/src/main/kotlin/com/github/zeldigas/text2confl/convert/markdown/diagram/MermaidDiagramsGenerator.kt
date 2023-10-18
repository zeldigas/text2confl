package com.github.zeldigas.text2confl.convert.markdown.diagram

import com.github.zeldigas.text2confl.convert.markdown.MermaidDiagramsConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path

class MermaidDiagramsGenerator(
    private val enabled: Boolean = true,
    override val defaultFileFormat: String = DEFAULT_FORMAT,
    private val command: String = DEFAULT_COMMAND,
    private val commandExecutor: CommandExecutor = OsCommandExecutor(),
    private val configFile: String? = null,
    private val cssFile: String? = null,
    private val puppeterConfig: String? = null
) : DiagramGenerator {
    companion object {
        const val DEFAULT_COMMAND = "mmdc"
        const val DEFAULT_FORMAT = "png"
        private val SUPPORTED_LANGUAGES = setOf("mermaid")
        private val SUPPORTED_FORMATS = setOf("png", "svg")
        private val PUPPETER_CONFIG_ENV = "T2C_PUPPEETER_CONFIG"

        private val logger = KotlinLogging.logger {}
    }

    constructor(config: MermaidDiagramsConfiguration, commandExecutor: CommandExecutor = OsCommandExecutor()) : this(
        config.enabled,
        config.defaultFormat,
        config.executable ?: DEFAULT_COMMAND,
        configFile = config.configFile,
        cssFile = config.cssFile,
        puppeterConfig = config.puppeeterConfig,
        commandExecutor = commandExecutor
    )

    override val supportedFileFormats: Set<String>
        get() = SUPPORTED_FORMATS

    override fun supports(lang: String): Boolean = SUPPORTED_LANGUAGES.contains(lang)

    override fun generate(source: String, target: Path, attributes: Map<String, String>): ImageInfo {
        val executable = cmd(command) {
            opt("--output", target.toString())
            opt("--outputFormat", resolveFormat(attributes))
            configFile?.let { opt("--configFile", it) }
            cssFile?.let { opt("--cssFile", it) }
            effectivePuppeeterConfig()?.let { opt("--puppeteerConfigFile", it) }
            flag("--quiet")

            stdin(source)
        }

        val result = commandExecutor.execute(executable)

        if (result.status != 0) {
            throw DiagramGenerationFailedException("$command execution returned non-zero exit code: ${result.status}.\n${result.output}")
        }
        return ImageInfo()
    }

    private fun effectivePuppeeterConfig(): String? = puppeterConfig ?: System.getenv(PUPPETER_CONFIG_ENV)

    override fun available(): Boolean {
        if (!enabled) return false
        if (!commandExecutor.commandAvailable(command)) return false

        val result = try {
            commandExecutor.execute(cmd(command) { flag("-V") })
        } catch (ex: Exception) {
            logger.debug(ex) { "Failed to execute command" }
            return false
        }
        return if (result.status == 0) {
            logger.info { "Mermaid version: ${result.output}" }
            true
        } else {
            false
        }
    }
}