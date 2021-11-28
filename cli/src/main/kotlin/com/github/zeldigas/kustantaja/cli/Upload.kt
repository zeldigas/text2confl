package com.github.zeldigas.kustantaja.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.long
import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.confclient.confluenceClient
import com.github.zeldigas.kustantaja.convert.confluence.LanguageMapper
import com.github.zeldigas.kustantaja.convert.universalConverter
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.URL

class Upload : CliktCommand(name = "upload", help = "Converts source files and uploads them to confluence") {

    private val confluenceUrl: URL by option("--confluence-url", envvar = "CONFLUENCE_URL").convert { URL(it) }.required()
    private val confluenceUser: String by option("--user", envvar = "CONFLUENCE_USER").required()
    private val confluencePassword: String by option("--password", envvar = "CONFLUENCE_PASSWORD")
        .prompt(requireConfirmation = true, hideInput = true)
    private val skipSsl: Boolean by option("--skip-ssl-verification")
        .flag("--no-skip-ssl-verification", default = false)

    private val spaceKey: String by option("--space", envvar = "CONFLUENCE_SPACE").required()
    private val parentId: Long? by option("--parent-id").long()
    private val parentTitle: String? by option("--parent")
    private val modificationCheck by option("--check-modification").enum<ChangeDetector>().default(ChangeDetector.HASH)
    private val removeOrphans: Boolean by option("--remove-orphans").flag("--keep-orphans", default = false)

    private val docs: File by option("--docs").file(canBeFile = true, canBeDir = true).required()

    override fun run() = runBlocking {
        val converter = universalConverter(spaceKey, LanguageMapper.forCloud())
        val result = if (docs.isFile) {
            listOf(converter.convertFile(docs.toPath()))
        } else {
            converter.convertDir(docs.toPath())
        }
        val confluenceClient = confluenceClient(confluenceUrl.toString(), confluenceUser, confluencePassword, skipSsl)
        val publishUnder = resolveParent(confluenceClient)
        ContentUploader(
            confluenceClient, "Automated upload", true,
            modificationCheck
        ).uploadPages(pages = result, spaceKey, publishUnder)
    }

    private suspend fun resolveParent(confluenceClient: ConfluenceClient): String {
        if (parentId != null) return parentId!!.toString()
        if (parentTitle != null) return confluenceClient.getPage(spaceKey, parentTitle!!).id

        return confluenceClient.describeSpace(spaceKey, listOf("homepage")).homepage?.id!!
    }
}