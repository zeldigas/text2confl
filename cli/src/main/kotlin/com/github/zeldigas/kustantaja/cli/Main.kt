package com.github.zeldigas.kustantaja.cli

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
            val userConfigDir = "$baseConfigDir/kustantaja"
            valueSource = ChainedValueSource(
                listOf(
                    PropertiesValueSource.from("kustantaja.properties"),
                    PropertiesValueSource.from("$userConfigDir/kustantaja.properties")
                )
            )
        }
    }

    override fun run() = Unit

}

fun main(args: Array<String>) {
    ConfluencePublisher()
        .subcommands(Convert())
        .subcommands(Upload())
        .main(args)
}