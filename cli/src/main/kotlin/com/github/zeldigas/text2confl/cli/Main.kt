package com.github.zeldigas.text2confl.cli

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.counted
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.sources.ChainedValueSource
import com.github.ajalt.clikt.sources.PropertiesValueSource
import com.github.zeldigas.text2confl.core.ServiceProviderImpl

class ConfluencePublisher : CliktCommand() {

    init {
        context {
            val baseConfigDir =
                System.getenv("XDG_CONFIG_HOME")?.ifBlank { null } ?: "${System.getProperty("user.home")}/.config"
            val userConfigDir = "$baseConfigDir/text2confl"
            valueSource = ChainedValueSource(
                listOf(
                    PropertiesValueSource.from("text2confl.properties"),
                    PropertiesValueSource.from("$userConfigDir/config.properties")
                )
            )
        }
    }

    val verbosityLevel by option("-v", help = "Enable verbose output").counted()

    override fun run() {
        configureLogging(verbosityLevel)
        currentContext.obj = ServiceProviderImpl()
    }

}

fun main(args: Array<String>) {
    ConfluencePublisher()
        .subcommands(Convert())
        .subcommands(Upload())
        .subcommands(DumpToMarkdown())
        .main(args)
}