package com.github.zeldigas.text2confl.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.github.zeldigas.confclient.ConfluenceClientConfig
import com.github.zeldigas.text2confl.cli.export.PageExporter
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.File

class DumpToMarkdown : CliktCommand(name = "export-to-md", help = "Exports confluence page to markdown file"),
    WithConfluenceServerOptions {
    override val confluenceUrl: Url by confluenceUrl().required()
    override val confluenceUser: String? by confluenceUser()
    override val confluencePassword: String? by confluencePassword()
    override val accessToken: String? by accessToken()
    override val skipSsl: Boolean? by skipSsl()
    val space: String? by confluenceSpace()
    private val pageId: String? by option("--page-id", help = "Id of page that you want to dump")
    private val pageTitle: String? by option(
        "--page-title",
        help = "Title of page that you want to dump. Has less priority over `page-id` option"
    )
    private val dest: File by option("--dest")
        .file(canBeFile = false, canBeDir = true)
        .default(File("."))

    private val serviceProvider: ServiceProvider by requireObject()

    override fun run() {
        try {
            runBlocking { dumpPage() }
        } catch (ex: Exception) {
            tryHandleException(ex)
        }
    }

    private suspend fun dumpPage() {
        val clientConfig = ConfluenceClientConfig(
            confluenceUrl,
            skipSsl ?: true,
            confluenceAuth
        )

        val client = serviceProvider.createConfluenceClient(clientConfig, false)
        val pageExporter = PageExporter(confluenceUrl, space, client)

        if (pageId != null) {
            pageExporter.exportPage(pageId!!, dest.toPath())
        } else {
            val space = this.space ?: parameterMissing("Space", "--space")
            val title = this.pageTitle!!
            pageExporter.exportPage(space, title, dest.toPath())
        }
    }

    private fun contentExpansions() = setOf(
        "metadata.labels",
        "children.attachment",
        "body.storage"
    )

    override fun askForSecret(prompt: String, requireConfirmation: Boolean): String? =
        prompt(prompt, hideInput = true, requireConfirmation = true)
}
