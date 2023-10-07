package com.github.zeldigas.text2confl.convert.markdown

import com.github.zeldigas.text2confl.convert.AttachmentCollector
import com.github.zeldigas.text2confl.convert.AttachmentsRegistry
import com.github.zeldigas.text2confl.convert.confluence.ReferenceProvider
import com.vladsch.flexmark.ast.*
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.ast.NodeVisitor
import com.vladsch.flexmark.util.ast.VisitHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path

class MarkdownAttachmentCollector(
    private val source: Path,
    referencesProvider: ReferenceProvider,
    attachmentsRegistry: AttachmentsRegistry
) {

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val attachmentsCollector = AttachmentCollector(referencesProvider, attachmentsRegistry)


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
        attachmentsCollector.collectRelativeToSourceFile(source, pathToFile, referenceName)
    }

}