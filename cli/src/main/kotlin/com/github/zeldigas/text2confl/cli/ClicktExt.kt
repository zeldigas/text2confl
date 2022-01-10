package com.github.zeldigas.text2confl.cli

import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.FlagOption
import com.github.ajalt.clikt.parameters.options.RawOption
import com.github.ajalt.clikt.parameters.options.switch

fun parameterMissing(what: String, cliOption: String, fileOption: String): Nothing {
    throw PrintMessage("$what is not specified. Use `$cliOption` option or `$fileOption` in config file", error = true)
}

fun RawOption.optionalFlag(vararg secondaryNames: String): FlagOption<Boolean?> {
    val allOptions = names.map { it to true } + secondaryNames.map { it to false }
    return switch(allOptions.toMap())
}
