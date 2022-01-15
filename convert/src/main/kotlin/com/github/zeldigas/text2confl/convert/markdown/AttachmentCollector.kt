package com.github.zeldigas.text2confl.convert.markdown

import com.github.zeldigas.text2confl.convert.confluence.ReferenceProvider
import com.vladsch.flexmark.ast.Image
import com.vladsch.flexmark.ast.ImageRef
import com.vladsch.flexmark.ast.Link
import com.vladsch.flexmark.ast.LinkRef
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.ast.NodeVisitor
import com.vladsch.flexmark.util.ast.VisitHandler
import mu.KotlinLogging
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

class AttachmentCollector(private val source: Path, private val referencesProvider: ReferenceProvider) {

    companion object {
        private val logger = KotlinLogging.logger {  }
    }

    private val parentDir: Path = source.parent ?: Paths.get(".")
    private val attachments = mutableMapOf<String, Path>()


    fun collectAttachments(ast: Node): Map<String, Path> {
        NodeVisitor(listOf(
            VisitHandler(Link::class.java) { tryCollect(it) },
            VisitHandler(LinkRef::class.java) { tryCollect(it, ast as Document) },
            VisitHandler(Image::class.java) { tryCollect(it) },
            VisitHandler(ImageRef::class.java) { tryCollect(it, ast as Document) }
        )).visit(ast)
        return attachments
    }

    private fun tryCollect(node: Link) {
        addFileIfExists(node.url.unescape())
    }

    private fun tryCollect(node: LinkRef, ast: Document) {
        val referenceNode = node.getReferenceNode(ast)
        if (referenceNode == null) {
            logger.debug { "Skipping link reference with no resolved url: $node" }
            return
        }
        addFileIfExists(referenceNode.url.unescape())
    }

    private fun tryCollect(node: Image) {
        addFileIfExists(node.url.unescape())
    }

    private fun tryCollect(node: ImageRef, ast: Document) {
        val referenceNode = node.getReferenceNode(ast)
        if (referenceNode == null) {
            logger.debug { "Skipping image reference with no resolved url: $node" }
            return
        }
        addFileIfExists(referenceNode.url.unescape())
    }

    private fun addFileIfExists(pathToFile: String) {
        if (!isLocal(pathToFile)) return
        if (referencesProvider.resolveReference(source, pathToFile) != null) return

        val file = parentDir.resolve(pathToFile).normalize()
        if (file.exists()) {
            logger.debug { "File exists, adding as attachment: $file" }
            attachments[pathToFile] = file
        } else {
            logger.warn { "File does not exist: $file" }
        }
    }

    private fun isLocal(pathToFile: String): Boolean {
        if (pathToFile.isEmpty()) return false

        return try {
            val uri = URI.create(pathToFile)
            uri.scheme == null
        } catch (e: Exception) {
            logger.trace(e) { "Failed to create uri from $pathToFile, considering as non-local" }
            false
        }
    }

}