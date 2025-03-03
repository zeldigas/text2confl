package com.github.zeldigas.text2confl.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.github.zeldigas.text2confl.core.ServiceProvider
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.File

class DumpToMarkdown : CliktCommand(name = "export-to-md"),
    WithConfluenceServerOptions {
    override val confluenceUrl: Url by confluenceUrl().required()
    override val confluenceUser: String? by confluenceUser()
    override val confluencePassword: String? by confluencePassword()
    override val accessToken: String? by accessToken()
    override val skipSsl: Boolean? by skipSsl()
    override val httpLogLevel: LogLevel by httpLoggingLevel()
    override val httpRequestTimeout: Long? by httpRequestTimeout()
    override val confluenceCloud: Boolean? by confluenceCloudFlag()

    val space: String? by confluenceSpace()
    private val pageId: String? by option("--page-id", help = "Id of page that you want to dump")
    private val pageTitle: String? by option(
        "--page-title",
        help = "Title of page that you want to dump. Has less priority over `page-id` option"
    )
    private val dest: File by option("--dest")
        .file(canBeFile = false, canBeDir = true)
        .default(File("."))
    private val assetsDir by option(
        "--assets-dir",
        help = "Directory relative to destination folder where attachments will be stored"
    )
    private val saveContentSource by option("--dump-also-storage-format")
        .flag("--no-dump-also-storage-format", default = false)

    private val serviceProvider: ServiceProvider by requireObject()

    override fun help(context: Context) = "Exports confluence page to markdown file"

    override fun run() {
        try {
            configureRequestLoggingIfEnabled()
            runBlocking { dumpPage() }
        } catch (ex: Exception) {
            tryHandleException(ex)
        }
    }

    private suspend fun dumpPage() {
        val clientConfig = httpClientConfig(confluenceUrl)

        val client = serviceProvider.createConfluenceClient(clientConfig, false)
        val pageExporter = serviceProvider.createPageExporter(client, saveContentSource)

        if (pageId != null) {
            pageExporter.exportPageContent(pageId!!, dest.toPath(), assetsDir)
        } else {
            val space = this.space ?: parameterMissing("Space", "--space")
            val title = this.pageTitle!!
            pageExporter.exportPageContent(space, title, dest.toPath(), assetsDir)
        }
    }

    override fun askForSecret(prompt: String, requireConfirmation: Boolean): String? =
        promptForSecret(prompt, requireConfirmation)
}
