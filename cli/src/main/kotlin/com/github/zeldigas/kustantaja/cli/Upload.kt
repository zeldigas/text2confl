package com.github.zeldigas.kustantaja.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.confclient.ConfluenceClientConfig
import com.github.zeldigas.confclient.PasswordAuth
import com.github.zeldigas.confclient.TokenAuth
import com.github.zeldigas.kustantaja.cli.config.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.File

private class UserCreds : OptionGroup("Confluence user credentials") {
    val confluenceUser: String by option("--user", envvar = "CONFLUENCE_USER").required()
    val confluencePassword: String? by option("--password", envvar = "CONFLUENCE_PASSWORD", help = "User password or personal API token")
}

class Upload : CliktCommand(name = "upload", help = "Converts source files and uploads them to confluence") {

    private val confluenceUrl: Url? by option(
        "--confluence-url", envvar = "CONFLUENCE_URL",
        help = "Address of confluence server. For Confluence cloud it is usually https://<site>.atlassian.net/wiki"
    ).convert { Url(it) }
    private val oauthToken: String? by option(
        "--oauth-token", envvar = "CONFLUENCE_OAUTH_TOKEN",
        help = "OAuth2 access token"
    )
    private val userCreds: UserCreds? by UserCreds().cooccurring()
    private val skipSsl: Boolean? by option(
        "--skip-ssl-verification",
        help = "If ssl checks should be skipped when connecting to server"
    )
        .optionalFlag("--no-skip-ssl-verification")

    private val spaceKey: String? by option(
        "--space", envvar = "CONFLUENCE_SPACE",
        help = "Destination confluence space"
    )
    private val parentId: String? by option("--parent-id", help = "Id of parent page where root pages should be added")
    private val parentTitle: String? by option(
        "--parent",
        help = "Title of parent page where root pages should be added. Has less priority over `parent-id` option"
    )
    private val modificationCheck by option(
        "--check-modification",
        help = "Strategy to check changes for existing pages: `hash` - by content hash stored as page property, `content` - by comparing content in storage format"
    ).enum<ChangeDetector> { it.name.lowercase() }
    private val removeOrphans: Boolean? by option(
        "--remove-orphans",
        help = "If pages that are not stored as code should be removed"
    ).optionalFlag("--keep-orphans")
    private val changeMessage: String? by option("-m", "--message", help = "Comment message for created/updated pages")
    private val notifyWatchers: Boolean? by option(
        "--notify-watchers",
        help = "If watchers should be notified about change"
    ).optionalFlag("--no-notify-watchers")
    private val editorVersion: EditorVersion? by option(
        "--editor-version",
        help = "Version of editor and page renderer on server. Autodected if not specified"
    ).enum<EditorVersion> { it.name.lowercase() }

    private val docs: File by option("--docs").file(canBeFile = true, canBeDir = true).required()

    private val serviceProvider: ServiceProvider by requireObject()

    override fun run() = runBlocking {
        val directoryStoredParams = readDirectoryConfig(docs.toPath());
        val uploadConfig = createUploadConfig(directoryStoredParams)
        val clientConfig = createClientConfig(directoryStoredParams)
        val conversionConfig = createConversionConfig(directoryStoredParams, clientConfig.server)
        val converter = serviceProvider.createConverter(uploadConfig.space, conversionConfig)
        val result = if (docs.isFile) {
            listOf(converter.convertFile(docs.toPath()))
        } else {
            converter.convertDir(docs.toPath())
        }
        val confluenceClient = serviceProvider.createConfluenceClient(clientConfig)
        val publishUnder = resolveParent(confluenceClient, uploadConfig, directoryStoredParams)

        val contentUploader = serviceProvider.createUploader(confluenceClient, uploadConfig, conversionConfig)
        contentUploader.uploadPages(pages = result, uploadConfig.space, publishUnder)
    }

    private fun createUploadConfig(configuration: DirectoryConfig): UploadConfig {
        return UploadConfig(
            space = spaceKey ?: configuration.space ?: parameterMissing("Space", "--space", "space"),
            removeOrphans = removeOrphans ?: configuration.removeOrphans,
            uploadMessage = changeMessage ?: "Automated upload by text2confl",
            notifyWatchers = notifyWatchers ?: configuration.notifyWatchers,
            modificationCheck = modificationCheck ?: configuration.modificationCheck
        )
    }

    private fun createClientConfig(configuration: DirectoryConfig): ConfluenceClientConfig {
        val server = confluenceUrl ?: configuration.server?.let { Url(it) }
        ?: parameterMissing("Confluence url", "--confluence-url", "server")
        val auth = when {
            oauthToken != null -> TokenAuth(oauthToken!!)
            userCreds != null -> passwordAuth(userCreds!!)
            else -> throw PrintMessage("Either access token or username/password should be specified", error = true)
        }
        return ConfluenceClientConfig(
            server = server,
            skipSsl = skipSsl ?: configuration.skipSsl,
            auth = auth
        )
    }

    private fun passwordAuth(creds: UserCreds): PasswordAuth {
        val password = creds.confluencePassword
            ?: TermUi.prompt("Enter password: ", hideInput = true, requireConfirmation = true)
            ?: throw PrintMessage("Password can't be null")
        return PasswordAuth(creds.confluenceUser, password)
    }

    private fun createConversionConfig(directoryConfig: DirectoryConfig, server: Url): ConverterConfig {
        val selectedVersion = editorVersion ?: directoryConfig.editorVersion
        return ConverterConfig(
            titlePrefix = directoryConfig.titlePrefix,
            titlePostfix = directoryConfig.titlePostfix,
            editorVersion = selectedVersion ?: inferFromUrl(server)
        )
    }

    private fun inferFromUrl(server: Url): EditorVersion {
        return if (server.host.endsWith(".atlassian.net", ignoreCase = true)) {
            EditorVersion.V2
        } else {
            EditorVersion.V1
        }
    }

    private suspend fun resolveParent(
        confluenceClient: ConfluenceClient,
        uploadConfig: UploadConfig,
        directoryConfig: DirectoryConfig
    ): String {
        val anyParentId = listOf(parentId, directoryConfig.defaultParentId).firstOrNull { it != null }
        if (anyParentId != null) return anyParentId
        val anyTitle = listOf(parentTitle, directoryConfig.defaultParent).firstOrNull { it != null }
        if (anyTitle != null) return confluenceClient.getPage(uploadConfig.space, anyTitle).id

        return confluenceClient.describeSpace(uploadConfig.space, listOf("homepage")).homepage?.id!!
    }
}