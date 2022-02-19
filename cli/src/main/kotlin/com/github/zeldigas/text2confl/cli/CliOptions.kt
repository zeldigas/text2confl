package com.github.zeldigas.text2confl.cli

import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.github.zeldigas.text2confl.cli.config.EditorVersion

fun ParameterHolder.editorVersion() = option(
    "--editor-version",
    help = "Version of editor and page renderer on server. Autodected if not specified"
).enum<EditorVersion> { it.name.lowercase() }

fun ParameterHolder.confluenceSpace() = option(
    "--space", envvar = "CONFLUENCE_SPACE",
    help = "Destination confluence space"
)

fun ParameterHolder.docsLocation() = option("--docs")
    .file(canBeFile = true, canBeDir = true).required()
    .help("File or directory with files to convert")

internal interface WithConversionOptions {
    val spaceKey: String?
    val editorVersion: EditorVersion?
}