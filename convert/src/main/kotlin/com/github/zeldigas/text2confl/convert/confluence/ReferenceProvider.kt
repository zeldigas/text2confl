package com.github.zeldigas.text2confl.convert.confluence

import com.github.zeldigas.text2confl.convert.PageHeader
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URLDecoder
import java.nio.file.Path
import kotlin.io.path.relativeTo

interface ReferenceProvider {
    fun resolveReference(source: Path, refTo: String): Reference?

    fun pathFromDocsRoot(source: Path): Path

    companion object {
        fun fromDocuments(basePath: Path, documents: Map<Path, PageHeader>): ReferenceProvider {
            return ReferenceProviderImpl(basePath, documents)
        }

        private val NOP_PROVIDER = object : ReferenceProvider {
            override fun resolveReference(source: Path, refTo: String): Reference? {
                return null
            }

            override fun pathFromDocsRoot(source: Path): Path {
                return source.fileName
            }
        }

        fun singleFile(): ReferenceProvider {
            return NOP_PROVIDER
        }
    }
}

sealed class Reference(open val target: String)
data class Xref(override val target: String, val anchor: String?) : Reference(target)
data class Anchor(override val target: String) : Reference(target)


class ReferenceProviderImpl(private val basePath: Path, documents: Map<Path, PageHeader>) :
    ReferenceProvider {

    companion object {
        private val URI_DETECTOR = "^[a-zA-Z][a-zA-Z0-9.+-]+:/{0,2}".toRegex(RegexOption.IGNORE_CASE)
        private val log = KotlinLogging.logger {  }
    }

    private val normalizedDocs =
        documents.map { (path, header) -> path.relativeTo(basePath).normalize() to header }.toMap()

    override fun resolveReference(source: Path, refTo: String): Reference? {
        if (URI_DETECTOR.find(refTo) != null) {
            log.debug { "$refTo detected as link in $source" }
            return null
        }
        val normalizedRef = URLDecoder.decode(refTo, "UTF-8")
        if (normalizedRef.startsWith("#")) return Anchor(normalizedRef.substring(1))

        val parts = normalizedRef.split("#", limit = 2)
        val ref = parts[0]
        val anchor = parts.getOrNull(1)

        val resolvedReference = source.resolveSibling(ref)
        val targetPath = if (resolvedReference.isAbsolute) resolvedReference else resolvedReference.relativeTo(basePath)

        val document = normalizedDocs[targetPath.normalize()]?.title ?: return null
        return Xref(document, anchor)
    }

    override fun pathFromDocsRoot(source: Path): Path {
        return source.relativeTo(basePath)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReferenceProviderImpl

        if (basePath != other.basePath) return false
        if (normalizedDocs != other.normalizedDocs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = basePath.hashCode()
        result = 31 * result + normalizedDocs.hashCode()
        return result
    }


}

