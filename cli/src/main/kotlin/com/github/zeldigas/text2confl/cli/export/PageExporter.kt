package com.github.zeldigas.text2confl.cli.export

import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.confclient.model.ConfluencePage
import com.github.zeldigas.text2confl.cli.sanitizeTitle
import com.github.zeldigas.text2confl.convert.markdown.export.HtmlToMarkdownConverter
import io.ktor.http.*
import java.io.OutputStreamWriter
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.writer

class PageExporter(confluenceUrl: Url, space: String?, private val client: ConfluenceClient) {

    companion object {
        private val CONTENT_EXTENSIONS = setOf(
            "metadata.labels",
            "children.attachment",
            "body.storage"
        )
    }

    private val converter = HtmlToMarkdownConverter(ConfluenceLinkResolverImpl(confluenceUrl, client, space))

    suspend fun exportPage(id: String, dest: Path) {
        exportPage(client.getPageById(id, CONTENT_EXTENSIONS), dest)
    }

    suspend fun exportPage(space: String, title: String, dest: Path) {
        exportPage(client.getPage(space, title, expansions = CONTENT_EXTENSIONS), dest)
    }

    private fun exportPage(page: ConfluencePage, dest: Path) {
        val content = page.body?.storage?.value!!
        println(content)
        val pageContent = converter.convert(content)

        dest.createDirectories()

        (dest / "${sanitizeTitle(page.title)}.md").writer().use { writer ->
            writeLabels(page, writer)
            writeHeader(writer, page)

            writer.write(pageContent)
        }

        val assetsDir = dest / "_assets"
        //todo dump attachments

    }

    private fun writeHeader(writer: OutputStreamWriter, page: ConfluencePage) {
        writer.appendLine("""h1. ${page.title}""")
        writer.appendLine()
    }

    private fun writeLabels(page: ConfluencePage, writer: OutputStreamWriter) {
        page.metadata?.labels?.results?.let { labels ->
            if (labels.isNotEmpty()) {
                writer.appendLine("---")
                writer.append("labels: ")
                labels.map { it.label }.filterNotNull().joinTo(writer)
                writer.appendLine()
                writer.appendLine("---")
                writer.appendLine()
            }
        }
    }


}