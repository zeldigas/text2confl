package com.github.zeldigas.text2confl.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.confclient.ConfluenceClientConfig
import com.github.zeldigas.confclient.PasswordAuth
import com.github.zeldigas.confclient.TokenAuth
import com.github.zeldigas.text2confl.cli.config.*
import com.github.zeldigas.text2confl.cli.upload.ChangeDetector
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

class Upload : CliktCommand(name = "upload", help = "Converts source files and uploads them to confluence"),
    WithConversionOptions {

    private val confluenceUrl: Url? by option(
        "--confluence-url", envvar = "CONFLUENCE_URL",
        help = "Address of confluence server. For Confluence cloud it is usually https://<site>.atlassian.net/wiki"
    ).convert { Url(it) }
    val confluenceUser: String? by option("--user", envvar = "CONFLUENCE_USER")
    val confluencePassword: String? by option(
        "--password",
        envvar = "CONFLUENCE_PASSWORD",
        help = "User password or personal API token provided instead of password (e.g. in Confluence Cloud)"
    )
    private val accessToken: String? by option(
        "--access-token", envvar = "CONFLUENCE_ACCESS_TOKEN",
        help = "Confluence api token. Used for token only authorization in api (NO username)"
    )
    private val skipSsl: Boolean? by option(
        "--skip-ssl-verification",
        help = "If ssl checks should be skipped when connecting to server"
    )
        .optionalFlag("--no-skip-ssl-verification")

    override val spaceKey: String? by confluenceSpace()
    private val parentId: String? by option("--parent-id", help = "Id of parent page where root pages should be added")
    private val parentTitle: String? by option(
        "--parent",
        help = "Title of parent page where root pages should be added. Has less priority over `parent-id` option"
    )
    private val modificationCheck by option(
        "--check-modification",
        help = "Strategy to check changes for existing pages: `hash` - by content hash stored as page property, `content` - by comparing content in storage format"
    ).enum<ChangeDetector> { it.name.lowercase() }
    private val tenant: String? by option("--tenant",
        help = "Tenant id for uploaded pages")
    private val removeOrphans: Cleanup? by option(
        "--remove-orphans",
        help = """What to do with child pages that are not managed by: 
            |1) managed - remove only pages that were previously managed by text2confl
            |2) all - remove pages
            |3) none - don't remove any pages""".trimMargin()
    ).enum<Cleanup> { it.name.lowercase() }
    private val changeMessage: String? by option("-m", "--message", help = "Comment message for created/updated pages")
    private val notifyWatchers: Boolean? by option(
        "--notify-watchers",
        help = "If watchers should be notified about change"
    ).optionalFlag("--no-notify-watchers")
    private val dryRun: Boolean by option("--dry", help = "Enables dry run simulation of documents upload")
        .flag("--no-dry")
    override val editorVersion: EditorVersion? by editorVersion()
    private val docs: File by docsLocation()

    private val serviceProvider: ServiceProvider by requireObject()

    override fun run() = runBlocking {
        try {
            tryUpload()
        } catch (ex: Exception) {
            tryHandleException(ex)
        }
    }

    private suspend fun tryUpload() {
        val directoryStoredParams = readDirectoryConfig(docs.toPath());
        val uploadConfig = createUploadConfig(directoryStoredParams)
        val clientConfig = createClientConfig(directoryStoredParams)
        val conversionConfig = createConversionConfig(directoryStoredParams, editorVersion, clientConfig.server)
        val converter = serviceProvider.createConverter(uploadConfig.space, conversionConfig)
        val result = if (docs.isFile) {
            listOf(converter.convertFile(docs.toPath()))
        } else {
            converter.convertDir(docs.toPath())
        }
        serviceProvider.createContentValidator().validate(result)
        val confluenceClient = serviceProvider.createConfluenceClient(clientConfig, dryRun)
        val publishUnder = resolveParent(confluenceClient, uploadConfig, directoryStoredParams)

        val contentUploader = serviceProvider.createUploader(confluenceClient, uploadConfig, conversionConfig)
        withContext(Dispatchers.Default) {
            contentUploader.uploadPages(pages = result, uploadConfig.space, publishUnder)
        }
    }

    private fun createUploadConfig(configuration: DirectoryConfig): UploadConfig {
        val orphanRemoval = if (docs.isFile) {
            Cleanup.None
        } else {
            removeOrphans ?: configuration.removeOrphans
        }
        return UploadConfig(
            space = spaceKey ?: configuration.space ?: parameterMissing("Space", "--space", "space"),
            removeOrphans = orphanRemoval,
            uploadMessage = changeMessage ?: "Automated upload by text2confl",
            notifyWatchers = notifyWatchers ?: configuration.notifyWatchers,
            modificationCheck = modificationCheck ?: configuration.modificationCheck,
            tenant = tenant ?: configuration.tenant
        )
    }

    private fun createClientConfig(configuration: DirectoryConfig): ConfluenceClientConfig {
        val server = confluenceUrl ?: configuration.server?.let { Url(it) }
        ?: parameterMissing("Confluence url", "--confluence-url", "server")
        val auth = when {
            accessToken != null && confluenceUser != null -> throw PrintMessage("Both access token and username/password specified, but only one of them allowed")
            accessToken != null -> TokenAuth(accessToken!!)
            confluenceUser != null -> passwordAuth(confluenceUser!!, confluencePassword)
            else -> throw PrintMessage("Either access token or username/password should be specified", error = true)
        }
        return ConfluenceClientConfig(
            server = server,
            skipSsl = skipSsl ?: configuration.skipSsl,
            auth = auth
        )
    }

    private fun passwordAuth(username: String, password: String?): PasswordAuth {
        val effectivePassword = password
            ?: prompt("Enter password: ", hideInput = true, requireConfirmation = true)
            ?: throw PrintMessage("Password can't be null")
        return PasswordAuth(username, effectivePassword)
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