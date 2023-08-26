package com.github.zeldigas.text2confl.cli

import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.github.zeldigas.confclient.ConfluenceAuth
import com.github.zeldigas.confclient.PasswordAuth
import com.github.zeldigas.confclient.TokenAuth
import com.github.zeldigas.text2confl.convert.EditorVersion
import io.ktor.http.*

fun ParameterHolder.confluenceUrl() = option(
    "--confluence-url", envvar = "CONFLUENCE_URL",
    help = "Address of confluence server. For Confluence cloud it is usually https://<site>.atlassian.net/wiki"
).convert { Url(it) }

fun ParameterHolder.confluenceUser() = option("--user", envvar = "CONFLUENCE_USER")
fun ParameterHolder.confluencePassword() = option(
    "--password",
    envvar = "CONFLUENCE_PASSWORD",
    help = "User password or personal API token provided instead of password (e.g. in Confluence Cloud)"
)

fun ParameterHolder.accessToken() = option(
    "--access-token", envvar = "CONFLUENCE_ACCESS_TOKEN",
    help = "Confluence api token. Used for token only authorization in api (NO username)"
)

fun ParameterHolder.skipSsl() = option(
    "--skip-ssl-verification",
    help = "If ssl checks should be skipped when connecting to server"
)
    .optionalFlag("--no-skip-ssl-verification")

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

internal interface WithConfluenceServerOptions {
    val confluenceUrl: Url?
    val confluenceUser: String?
    val confluencePassword: String?
    val accessToken: String?
    val skipSsl: Boolean?

    val confluenceAuth: ConfluenceAuth
        get() = when {
            accessToken != null && confluenceUser != null -> throw PrintMessage("Both access token and username/password specified, but only one of them allowed")
            accessToken != null -> TokenAuth(accessToken!!)
            confluenceUser != null -> passwordAuth(confluenceUser!!, confluencePassword)
            else -> throw PrintMessage("Either access token or username/password should be specified", printError = true)
        }

    private fun passwordAuth(username: String, password: String?): PasswordAuth {
        val effectivePassword = password
            ?: askForSecret("Enter password: ", requireConfirmation = true)
            ?: throw PrintMessage("Password can't be null")
        return PasswordAuth(username, effectivePassword)
    }

    fun askForSecret(prompt: String, requireConfirmation: Boolean = true): String?
}

internal interface WithConversionOptions {
    val spaceKey: String?
    val editorVersion: EditorVersion?
}