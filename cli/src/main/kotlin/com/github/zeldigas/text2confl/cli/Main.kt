package com.github.zeldigas.text2confl.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.sources.ChainedValueSource
import com.github.ajalt.clikt.sources.PropertiesValueSource

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

    override fun run() {
        currentContext.obj = ServiceProviderImpl()
    }

}

fun main(args: Array<String>) {
    ConfluencePublisher()
        .subcommands(Convert())
        .subcommands(Upload())
        .main(args)
}