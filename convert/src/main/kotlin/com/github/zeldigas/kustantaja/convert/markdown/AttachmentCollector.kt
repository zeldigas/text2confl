package com.github.zeldigas.kustantaja.convert.markdown

import com.vladsch.flexmark.ast.Image
import com.vladsch.flexmark.ast.Link
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.ast.NodeVisitor
import com.vladsch.flexmark.util.ast.VisitHandler
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

class AttachmentCollector(source: Path) {

    private val parentDir: Path = source.parent ?: Paths.get(".")
    private val attachments = mutableMapOf<String, Path>()


    fun collectAttachments(ast: Node): Map<String, Path> {
        NodeVisitor(listOf(
            VisitHandler(Link::class.java) { tryCollect(it) },
            VisitHandler(Image::class.java) { tryCollect(it) }
        )).visit(ast)
        return attachments
    }

    private fun tryCollect(node: Link) {
        addFileIfExists(node.url.unescape())
    }

    private fun tryCollect(node: Image) {
        addFileIfExists(node.url.unescape())
    }

    private fun addFileIfExists(pathToFile: String) {
        if (!isLocal(pathToFile)) return

        val file = parentDir.resolve(pathToFile).normalize()
        if (file.exists()) {
            println("File exists, adding as attachment: $file")
            attachments[pathToFile] = file
        } else {
            println("File does not exist: $file")
        }
    }

    private fun isLocal(pathToFile: String): Boolean {
        return try {
            val uri = URI.create(pathToFile)
            uri.scheme == null
        } catch (e: Exception) {
            false
        }
    }

}