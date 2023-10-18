package com.github.zeldigas.text2confl.convert.confluence

import com.github.zeldigas.text2confl.convert.PageHeader
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URL
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.regex.Matcher
import java.util.regex.Pattern
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
        private const val URI_DETECTOR = "^(https?|ftp)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;'()]*[-a-zA-Z0-9+&@#/%=~_|]"
        private const val MAILTO_DETECTOR = "mailto:"
        private const val LOCALHOST_DETECTOR = "localhost:"

        private val logger = KotlinLogging.logger {}
    }
    fun isValid(url: String): Boolean {
        val pattern = Pattern.compile(URI_DETECTOR, Pattern.CASE_INSENSITIVE);
        val matcher = pattern.matcher(url.trim());
        return matcher.matches();
    }

    private val normalizedDocs =
        documents.map { (path, header) -> path.relativeTo(basePath).normalize() to header }.toMap()

    override fun resolveReference(source: Path, refTo: String): Reference? {

        if (refTo.startsWith(MAILTO_DETECTOR)) return null
        if (refTo.startsWith(LOCALHOST_DETECTOR)) return null
        if (isValid(refTo)) return null
        if (refTo.startsWith("#")) return Anchor(refTo.substring(1))

        val parts = refTo.split("#", limit = 2)
        val ref = parts[0]
        val anchor = parts.getOrNull(1)

        try {

            val targetPath = source.resolveSibling(ref).relativeTo(basePath).normalize()
            val document = normalizedDocs[targetPath]?.title ?: return null
            return Xref(document, anchor)
        } catch (ex: InvalidPathException) {
            logger.error { "Failed to resolve : $refTo  from $source" }
            throw ex
        }
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
