package com.github.zeldigas.text2confl.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.NullableOption
import com.github.ajalt.clikt.parameters.options.RawOption
import com.github.ajalt.clikt.parameters.options.nullableFlag
import com.github.ajalt.mordant.terminal.ConfirmationPrompt
import com.github.ajalt.mordant.terminal.StringPrompt
import com.github.ajalt.mordant.terminal.prompt
import com.github.zeldigas.confclient.BaseConfluenceException
import com.github.zeldigas.text2confl.convert.ConversionException
import com.github.zeldigas.text2confl.convert.ConversionFailedException
import com.github.zeldigas.text2confl.core.ContentValidationFailedException
import com.github.zeldigas.text2confl.core.upload.ContentCleanupException
import com.github.zeldigas.text2confl.core.upload.ContentUploadException
import com.github.zeldigas.text2confl.core.upload.InvalidTenantException
import com.github.zeldigas.text2confl.core.upload.PageNotFoundException
import com.github.zeldigas.text2confl.core.upload.PageOperationException

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
    fun describeErrorWithCause(ex: Exception): String = buildString {
        append(ex.message)
        val cause = ex.cause
        when (cause) {
            null -> {}
            is BaseConfluenceException-> {
                append("\n (cause: ${cause.message})")
            }
            is PageNotFoundException -> {
                append("\n cause: Page with '${cause.title}' not found in space ${cause.space}")
            }
            else -> {
                append("\n cause: ${cause}")
            }
        }
    }
    when (ex) {
        is InvalidTenantException -> error(ex.message!!)
        is ConversionException -> error(ex.message!!)
        is PageOperationException -> error(ex.message!!)
        is ContentUploadException -> error(describeErrorWithCause(ex))
        is ContentCleanupException -> error(describeErrorWithCause(ex))
        is ConversionFailedException -> {
            error("Failed to convert ${ex.file}: ${describeErrorWithCause(ex)}")
        }

        is ContentValidationFailedException -> {
            val issues = ex.errors.mapIndexed { index, error -> "${index + 1}. $error" }.joinToString(separator = "\n")
            error("Some pages content is invalid:\n${issues}")
        }

        else -> throw ex
    }
}

private fun error(message: String): Nothing {
    throw PrintMessage(message, printError = true, statusCode = 1)
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
