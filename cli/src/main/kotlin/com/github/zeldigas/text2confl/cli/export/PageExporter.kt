package com.github.zeldigas.text2confl.cli.export

import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.confclient.model.Attachment
import com.github.zeldigas.confclient.model.ConfluencePage
import com.github.zeldigas.text2confl.cli.sanitizeTitle
import com.github.zeldigas.text2confl.convert.markdown.export.HtmlToMarkdownConverter
import java.io.OutputStreamWriter
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.writer

class PageExporter(internal val client: ConfluenceClient, internal val saveContentSource: Boolean) {

    companion object {
        internal val CONTENT_EXTENSIONS = setOf(
            "metadata.labels",
            "children.attachment",
            "body.storage",
            "space"
        )
    }

    suspend fun exportPageContent(id: String, destinationDir: Path, assetsLocation: String?) {
        val page = client.getPageById(id, CONTENT_EXTENSIONS)
        exportPageWithAttachments(page, destinationDir, assetsLocation)
    }

    suspend fun exportPageContent(space: String, title: String, destinationDir: Path, assetsLocation: String?) {
        val page = client.getPage(space, title, expansions = CONTENT_EXTENSIONS)
        exportPageWithAttachments(page, destinationDir, assetsLocation)
    }

    private suspend fun exportPageWithAttachments(page: ConfluencePage, destinationDir: Path, assetsLocation: String?) {
        destinationDir.createDirectories()

        val attachmentDir = assetsLocation?.let { destinationDir / it } ?: destinationDir
        val space = page.space?.key!!
        val converter = HtmlToMarkdownConverter(ConfluenceLinkResolverImpl(client, space), assetsLocation ?: "")

        val attachments = page.children?.attachment?.let { client.fetchAllAttachments(it) } ?: emptyList()
        exportPageContent(converter, page, attachments, destinationDir, Path.of(assetsLocation ?: ""))
        if (attachments.isNotEmpty()) {
            attachmentDir.createDirectories()
            attachments.forEach { downloadTo(it, attachmentDir) }
        }
    }

    private suspend fun downloadTo(it: Attachment, attachmentDir: Path) {
        client.downloadAttachment(it, attachmentDir / it.title)
    }

    private fun exportPageContent(
        converter: HtmlToMarkdownConverter,
        page: ConfluencePage,
        attachments: List<Attachment>,
        dest: Path,
        attachmentDir: Path
    ) {
        val content = page.body?.storage?.value!!
        val pageContent = converter.convert(content)

        val sanitizedTitle = sanitizeTitle(page.title)
        (dest / "$sanitizedTitle.md").writer().use { writer ->
            writeLabels(page, writer)
            writeHeader(writer, page)

            writer.write(pageContent)

            if (attachments.isNotEmpty()) {
                attachments.forEach { attachment ->
                    writer.appendLine().appendLine()
                    writer.append('[')
                    writer.write(attachment.title)
                    writer.append("]: ")
                    writer.append("${attachmentDir / attachment.title}")
                }
            }
        }
        if (saveContentSource) {
            (dest / "$sanitizedTitle.html").writer().use { it.write(content) }
        }
    }

    private fun writeHeader(writer: OutputStreamWriter, page: ConfluencePage) {
        writer.appendLine("""# ${page.title}""")
        writer.appendLine()
    }

    private fun writeLabels(page: ConfluencePage, writer: OutputStreamWriter) {
        page.metadata?.labels?.results?.let { labels ->
            if (labels.isNotEmpty()) {
                writer.appendLine("---")
                writer.append("labels: ")
                labels.joinTo(writer) { it.label ?: it.name }
                writer.appendLine()
                writer.appendLine("---")
                writer.appendLine()
            }
        }
    }

}