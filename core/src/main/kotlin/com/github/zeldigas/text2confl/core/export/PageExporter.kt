package com.github.zeldigas.text2confl.core.export

import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.confclient.SimplePageLoadOptions
import com.github.zeldigas.confclient.model.Attachment
import com.github.zeldigas.confclient.model.ConfluencePage
import com.github.zeldigas.text2confl.convert.Page
import com.github.zeldigas.text2confl.convert.markdown.export.ConfluenceLinksResolver
import com.github.zeldigas.text2confl.convert.markdown.export.HtmlToMarkdownConverter
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.OutputStreamWriter
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.writer

class PageExporter(internal val client: ConfluenceClient, internal val saveContentSource: Boolean) {

    companion object {
        internal val CONTENT_EXTENSIONS = setOf(
            SimplePageLoadOptions.Labels, SimplePageLoadOptions.Attachments, SimplePageLoadOptions.Content, SimplePageLoadOptions.Space
        )
        internal const val DEFAULT_TREE_ASSETS_DIR = "_assets"
        private val logger = KotlinLogging.logger {}
    }

    suspend fun exportPageContent(id: String, destinationDir: Path, assetsLocation: String?) {
        val page = client.getPageById(id, CONTENT_EXTENSIONS)
        exportSinglePage(page, destinationDir, assetsLocation)
    }

    suspend fun exportPageContent(space: String, title: String, destinationDir: Path, assetsLocation: String?) {
        val page = client.getPage(space, title, loadOptions = CONTENT_EXTENSIONS)
        exportSinglePage(page, destinationDir, assetsLocation)
    }

    /**
     * Exports page with given [id] and all its descendants, using text2confl directory layout:
     * page is stored in `<title>.md` and its children in `<title>` directory next to it.
     *
     * Attachments of every page are stored in `<assetsLocation>/<page file name>` directory. Relative [assetsLocation]
     * is resolved against directory of exported page. Links between exported pages are converted to relative links.
     */
    suspend fun exportPageTree(
        id: String, destinationDir: Path, assetsLocation: String?,
        tracker: ExportProgressTracker = ExportProgressTracker.NOP
    ) {
        val root = client.getPageById(id, setOf(SimplePageLoadOptions.Space))
        exportTree(root, destinationDir, assetsLocation, tracker)
    }

    suspend fun exportPageTree(
        space: String, title: String, destinationDir: Path, assetsLocation: String?,
        tracker: ExportProgressTracker = ExportProgressTracker.NOP
    ) {
        val root = client.getPage(space, title, loadOptions = setOf(SimplePageLoadOptions.Space))
        exportTree(root, destinationDir, assetsLocation, tracker)
    }

    private suspend fun exportSinglePage(page: ConfluencePage, destinationDir: Path, assetsLocation: String?) {
        val space = page.space?.key!!
        val target = PageTarget(
            mdFile = destinationDir / "${sanitizeTitle(page.title)}.md",
            attachmentsDir = assetsLocation?.let { destinationDir / it } ?: destinationDir,
            attachmentsRef = Path.of(assetsLocation ?: "")
        )
        exportPageWithAttachments(
            page, target, ConfluenceLinkResolverImpl(client, space), ConfluenceUserResolverImpl(client)
        )
    }

    private suspend fun exportTree(
        root: ConfluencePage,
        destinationDir: Path,
        assetsLocation: String?,
        tracker: ExportProgressTracker
    ) {
        val space = root.space?.key!!
        val rootNode = ExportNode(root.id, root.title, destinationDir / "${sanitizeTitle(root.title)}.md")
        tracker.collectingTree(root.title)
        collectChildren(rootNode)

        val nodes = rootNode.flatten()
        tracker.treeCollected(nodes.size)
        val pagesLocation = nodes.associate { TreeLinkResolver.Key(space, it.title) to it.mdFile }

        val linkResolver = ConfluenceLinkResolverImpl(client, space)
        val userResolver = ConfluenceUserResolverImpl(client)
        val assets = Path.of(assetsLocation ?: DEFAULT_TREE_ASSETS_DIR)

        nodes.forEachIndexed { index, node ->
            logger.debug { "Exporting page ${node.title} to ${node.mdFile}" }
            val page = client.getPageById(node.id, CONTENT_EXTENSIONS)
            val pageDir = node.mdFile.parent
            val pageAssetsDirName = withoutSpaces(node.mdFile.nameWithoutExtension)
            val attachmentsDir = pageDir.resolve(assets).resolve(pageAssetsDirName)
            val target = PageTarget(
                mdFile = node.mdFile,
                attachmentsDir = attachmentsDir,
                attachmentsRef = if (assets.isAbsolute) pageDir.toAbsolutePath()
                    .relativize(attachmentsDir.toAbsolutePath()) else assets / pageAssetsDirName
            )
            exportPageWithAttachments(
                page, target, TreeLinkResolver(pagesLocation, space, pageDir, linkResolver), userResolver
            )
            tracker.pageExported(index + 1, nodes.size, node.title, node.mdFile)
        }
        tracker.exportCompleted(nodes.size)
    }

    private suspend fun collectChildren(node: ExportNode) {
        val childrenDir = node.mdFile.parent / node.mdFile.nameWithoutExtension
        val usedNames = mutableSetOf<String>()
        for (child in client.findChildPages(node.id)) {
            val baseName = sanitizeTitle(child.title)
            var name = baseName
            var counter = 2
            while (!usedNames.add(name.lowercase())) {
                name = "${baseName}_${counter++}"
            }
            if (name != baseName) {
                logger.warn { "Page ${child.title} file name clashes with sibling page, using $name.md instead" }
            }
            val childNode = ExportNode(child.id, child.title, childrenDir / "$name.md")
            node.children.add(childNode)
            collectChildren(childNode)
        }
    }

    private class ExportNode(
        val id: String,
        val title: String,
        val mdFile: Path,
        val children: MutableList<ExportNode> = mutableListOf()
    ) {
        fun flatten(): List<ExportNode> = listOf(this) + children.flatMap { it.flatten() }
    }

    private data class PageTarget(
        val mdFile: Path,
        val attachmentsDir: Path,
        val attachmentsRef: Path
    )

    private suspend fun exportPageWithAttachments(
        page: ConfluencePage,
        target: PageTarget,
        linksResolver: ConfluenceLinksResolver,
        userResolver: ConfluenceUserResolverImpl
    ) {
        target.mdFile.parent.createDirectories()

        val converter = HtmlToMarkdownConverter(
            linksResolver,
            target.attachmentsRef.toString(),
            userResolver
        )

        val attachments = page.attachments?.let { client.fetchAllAttachments(it) } ?: emptyList()
        val attachmentFiles = attachmentFileNames(attachments)
        exportPageContent(converter, page, attachmentFiles, target)
        if (attachments.isNotEmpty()) {
            target.attachmentsDir.createDirectories()
            attachmentFiles.forEach { (attachment, fileName) ->
                client.downloadAttachment(attachment, target.attachmentsDir / fileName)
            }
        }
    }

    /**
     * Local file names for attachments. Spaces are replaced, because local file references with spaces are not
     * supported in markdown. Attachment name in Confluence is preserved via reference name.
     */
    private fun attachmentFileNames(attachments: List<Attachment>): List<Pair<Attachment, String>> {
        val used = mutableSetOf<String>()
        return attachments.map { attachment ->
            val base = withoutSpaces(attachment.title)
            var name = base
            var counter = 2
            while (!used.add(name.lowercase())) {
                name = "${base.substringBeforeLast('.')}_${counter++}" +
                        (if ('.' in base) ".${base.substringAfterLast('.')}" else "")
            }
            attachment to name
        }
    }

    private fun withoutSpaces(name: String) = name.replace(' ', '_')

    private fun exportPageContent(
        converter: HtmlToMarkdownConverter,
        page: ConfluencePage,
        attachments: List<Pair<Attachment, String>>,
        target: PageTarget
    ) {
        val content = page.body?.storage?.value!!
        val pageContent = converter.convert(content)

        target.mdFile.writer().use { writer ->
            writeLabels(page, writer)
            writeHeader(writer, page)

            writer.write(pageContent)

            if (attachments.isNotEmpty()) {
                attachments.forEach { (attachment, fileName) ->
                    writer.appendLine().appendLine()
                    writer.append('[')
                    writer.write(attachment.title)
                    writer.append("]: ")
                    writer.append((target.attachmentsRef / fileName).joinToString("/"))
                }
            }
        }
        if (saveContentSource) {
            (target.mdFile.parent / "${target.mdFile.nameWithoutExtension}.html").writer().use { it.write(content) }
        }
    }

    private fun writeHeader(writer: OutputStreamWriter, page: ConfluencePage) {
        writer.appendLine("""# ${page.title}""")
        writer.appendLine()
    }

    private fun writeLabels(page: ConfluencePage, writer: OutputStreamWriter) {
        page.labels?.let { labels ->
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

fun sanitizeTitle(page: Page) = sanitizeTitle(page.content.header.title)
fun sanitizeTitle(title: String) = title.replace("""[^a-zA-Z0-9._ -]+""".toRegex(), "_")
