package com.github.zeldigas.text2confl.cli

import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.FlagOption
import com.github.ajalt.clikt.parameters.options.RawOption
import com.github.ajalt.clikt.parameters.options.switch
import com.github.zeldigas.text2confl.cli.upload.InvalidTenantException
import com.github.zeldigas.text2confl.convert.ConversionFailedException
import com.github.zeldigas.text2confl.convert.FileDoesNotExistException

fun parameterMissing(what: String, cliOption: String, fileOption: String): Nothing {
    throw PrintMessage("$what is not specified. Use `$cliOption` option or `$fileOption` in config file", error = true)
}

fun RawOption.optionalFlag(vararg secondaryNames: String): FlagOption<Boolean?> {
    val allOptions = names.map { it to true } + secondaryNames.map { it to false }
    return switch(allOptions.toMap())
}

fun tryHandleException(ex: Exception) : Nothing {
    when (ex) {
        is InvalidTenantException -> throw PrintMessage(ex.message!!, error = true)
        is FileDoesNotExistException -> throw PrintMessage(ex.message!!, error = true)
        is ConversionFailedException -> {
            val reason = buildString {
                append(ex.message)
                if (ex.cause != null) {
                    append(" (cause: ${ex.cause})")
                }
            }
            throw PrintMessage("Failed to convert ${ex.file}: $reason", error = true)
        }
        is ContentValidationFailedException -> {
            val issues = ex.errors.mapIndexed { index, error -> "${index + 1}. $error"}.joinToString(separator = "\n")
            throw PrintMessage("Some pages content is invalid:\n${issues}", error = true)
        }
        else -> throw ex
    }
}
