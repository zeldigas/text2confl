package com.github.zeldigas.text2confl.convert.markdown.diagram

import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.markdown.DiagramsConfiguration
import com.github.zeldigas.text2confl.convert.toBase64
import java.nio.file.Path
import java.security.MessageDigest

fun interface DiagramMakers {
    companion object {
        val NOP: DiagramMakers = DiagramMakers { null }
    }

    fun find(lang: String): DiagramMaker?
}

class DiagramMaker(
    val generator: DiagramGenerator,
    val baseDir: Path,
) {
    fun toDiagram(script: String, attributes: Map<String, String>, pathPrefix: Path?): Pair<Attachment, ImageInfo> {
        TODO()
    }

    private val String.contentHash: String
        get() {
            val sha256 = MessageDigest.getInstance("SHA-256")
            sha256.update(this.toByteArray())
            return toBase64(sha256.digest())
        }

//    private fun desiredFilename(script: String, attributes: Map<String, String>): Path {
//        val name = attributes["target"] ?: script.contentHash
//        return if (location != null) {
//            convertingContext.referenceProvider.pathFromDocsRoot(location).resolve("$name.svg")
//        } else {
//            Path.of("$name.svg")
//        }
//    }
}

fun createDiagramMakers(config: DiagramsConfiguration): DiagramMakers {
    return DiagramMakers.NOP//todo
}