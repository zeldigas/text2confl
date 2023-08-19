package com.github.zeldigas.text2confl.convert.markdown

import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.AttachmentsRegistry
import com.github.zeldigas.text2confl.convert.confluence.ReferenceProvider
import com.vladsch.flexmark.ast.*
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.ast.NodeVisitor
import com.vladsch.flexmark.util.ast.VisitHandler
import mu.KotlinLogging
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

class AttachmentCollector(
    private val source: Path,
    private val referencesProvider: ReferenceProvider,
    private val attachmentsRegistry: AttachmentsRegistry
) {

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val parentDir: Path = source.parent ?: Paths.get(".")


    fun collectAttachments(ast: Node) {
        NodeVisitor(listOf(
            VisitHandler(Link::class.java) { tryCollect(it) },
            VisitHandler(LinkRef::class.java) { tryCollect(it, ast as Document) },
            VisitHandler(Image::class.java) { tryCollect(it) },
            VisitHandler(ImageRef::class.java) { tryCollect(it, ast as Document) },
            VisitHandler(Reference::class.java) { tryCollect(it) }
        )).visit(ast)
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
        addFileIfExists(referenceNode.url.unescape(), referenceNode.reference.toString())
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
        addFileIfExists(referenceNode.url.unescape(), referenceNode.reference.toString())
    }

    private fun tryCollect(reference: Reference) {
        addFileIfExists(reference.url.unescape(), reference.reference.toString())
    }

    private fun addFileIfExists(pathToFile: String, referenceName: String? = null) {
        if (!isLocal(pathToFile)) return

        val effectiveName = referenceName ?: pathToFile

        if (attachmentsRegistry.hasRef(effectiveName)) return;
        if (referencesProvider.resolveReference(source, pathToFile) != null) return

        val file = parentDir.resolve(pathToFile).normalize()
        if (file.exists()) {
            logger.debug { "File exists, adding as attachment: $file with ref $effectiveName" }
            attachmentsRegistry.register(effectiveName, Attachment.fromLink(effectiveName, file))
        } else {
            logger.warn { "Unresolved local ref in [${source}], ref=${referenceName ?: "(inline)"}. File does not exist: $file" }
        }
    }

    private fun isLocal(pathToFile: String): Boolean {
        if (pathToFile.isEmpty() || pathToFile.startsWith("#")) return false

        return try {
            val uri = URI.create(pathToFile)
            uri.scheme == null
        } catch (e: Exception) {
            logger.trace(e) { "Failed to create uri from $pathToFile, considering as non-local" }
            false
        }
    }

}