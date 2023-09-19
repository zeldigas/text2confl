package com.github.zeldigas.text2confl.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.NullableOption
import com.github.ajalt.clikt.parameters.options.RawOption
import com.github.ajalt.clikt.parameters.options.nullableFlag
import com.github.ajalt.mordant.terminal.ConfirmationPrompt
import com.github.ajalt.mordant.terminal.StringPrompt
import com.github.zeldigas.text2confl.convert.ConversionFailedException
import com.github.zeldigas.text2confl.convert.FileDoesNotExistException
import com.github.zeldigas.text2confl.core.ContentValidationFailedException
import com.github.zeldigas.text2confl.core.upload.InvalidTenantException

fun parameterMissing(what: String, cliOption: String, fileOption: String): Nothing {
    throw PrintMessage(
        "$what is not specified. Use `$cliOption` option or `$fileOption` in config file",
        printError = true
    )
}

fun parameterMissing(what: String, cliOption: String): Nothing {
    throw PrintMessage("$what is not specified. Use `$cliOption` option", printError = true)
}


fun RawOption.optionalFlag(vararg secondaryNames: String): NullableOption<Boolean, Boolean> {
    return nullableFlag(*secondaryNames)
}

fun tryHandleException(ex: Exception): Nothing {
    when (ex) {
        is InvalidTenantException -> throw PrintMessage(ex.message!!, printError = true)
        is FileDoesNotExistException -> throw PrintMessage(ex.message!!, printError = true)
        is ConversionFailedException -> {
            val reason = buildString {
                append(ex.message)
                if (ex.cause != null) {
                    append(" (cause: ${ex.cause})")
                }
            }
            throw PrintMessage("Failed to convert ${ex.file}: $reason", printError = true)
        }

        is ContentValidationFailedException -> {
            val issues = ex.errors.mapIndexed { index, error -> "${index + 1}. $error" }.joinToString(separator = "\n")
            throw PrintMessage("Some pages content is invalid:\n${issues}", printError = true)
        }

        else -> throw ex
    }
}

fun CliktCommand.promptForSecret(prompt: String, requireConfirmation: Boolean): String? {
    return if (requireConfirmation) {
        ConfirmationPrompt.create(prompt, "Repeat for confirmation: ") {
            StringPrompt(it, terminal, hideInput = true)
        }.ask()
    } else {
        return terminal.prompt(prompt, hideInput = true)
    }
}
