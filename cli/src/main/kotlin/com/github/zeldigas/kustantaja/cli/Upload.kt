package com.github.zeldigas.kustantaja.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.long
import com.github.zeldigas.kustantaja.convert.universalConverter
import org.apache.http.impl.client.HttpClients
import org.sahli.asciidoc.confluence.publisher.client.http.ConfluenceRestClient
import java.io.File
import java.net.URL

class Upload : CliktCommand(name = "upload", help = "Converts source files and uploads them to confluence") {

    val confluenceUrl: URL by option("--confluence-url", envvar = "CONFLUENCE_URL").convert { URL(it) }.required()
    val confluenceUser: String by option("--user", envvar = "CONFLUENCE_USER").required()
    val confluencePassword: String by option("--password", envvar = "CONFLUENCE_PASSWORD")
        .prompt(requireConfirmation = true, hideInput = true)
    val skipSsl: Boolean by option("--skip-ssl-verification")
        .flag("--no-skip-ssl-verification", default = false)

    val spaceKey: String? by option("--space", envvar = "CONFLUENCE_SPACE")
    val parentId: Long? by option("--parent-id").long()
    val parentTitle: String? by option("--parent")
    val removeOrphans: Boolean by option("--remove-orphans").flag("--keep-orphans", default = false)

    val docs: File by option("--docs").file(canBeFile = true, canBeDir = true).required()

    override fun run() {
        val converter = universalConverter()
        val result = if (docs.isFile) {
            listOf(converter.convertFile(docs.toPath()))
        } else {
            converter.convertDir(docs.toPath())
        }
        val confluenceClient = ConfluenceRestClient(
            confluenceUrl.toString(),
            HttpClients.createDefault(),
            null,
            confluenceUser,
            confluencePassword
        )
        val publishUnder = resolveParent(confluenceClient)
        ContentUploader(confluenceClient, "Automated upload", true)
            .uploadPages(pages = result, spaceKey!!, publishUnder)
    }

    private fun resolveParent(confluenceClient: ConfluenceRestClient): String {
        if (parentId != null) return parentId!!.toString()

//        todo implement proper lookup of root page in space
        return confluenceClient.getPageByTitle(spaceKey!!, parentTitle ?: "Testing rig")
    }
}