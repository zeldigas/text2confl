package com.github.zeldigas.text2confl.cli

import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.NullableOption
import com.github.ajalt.clikt.parameters.options.RawOption
import com.github.ajalt.clikt.parameters.options.nullableFlag
import com.github.zeldigas.text2confl.cli.upload.InvalidTenantException
import com.github.zeldigas.text2confl.convert.ConversionFailedException
import com.github.zeldigas.text2confl.convert.FileDoesNotExistException

fun parameterMissing(what: String, cliOption: String, fileOption: String): Nothing {
    throw PrintMessage("$what is not specified. Use `$cliOption` option or `$fileOption` in config file", printError = true)
}

fun parameterMissing(what: String, cliOption: String): Nothing {
    throw PrintMessage("$what is not specified. Use `$cliOption` option", printError = true)
}


fun RawOption.optionalFlag(vararg secondaryNames: String): NullableOption<Boolean, Boolean> {
    return nullableFlag(*secondaryNames)
}

fun tryHandleException(ex: Exception) : Nothing {
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
            val issues = ex.errors.mapIndexed { index, error -> "${index + 1}. $error"}.joinToString(separator = "\n")
            throw PrintMessage("Some pages content is invalid:\n${issues}", printError = true)
        }
        else -> throw ex
    }
}
