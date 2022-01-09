package com.github.zeldigas.text2confl.convert.confluence

import com.github.zeldigas.text2confl.convert.PageHeader
import java.nio.file.Path
import kotlin.io.path.relativeTo

interface ReferenceProvider {
    fun resolveReference(source: Path, refTo: String): Reference?

    companion object {
        fun fromDocuments(basePath: Path, documents: Map<Path, PageHeader>): ReferenceProvider {
            return ReferenceProviderImpl(basePath, documents)
        }

        fun nop(): ReferenceProvider {
            return object : ReferenceProvider {
                override fun resolveReference(source: Path, refTo: String): Reference? {
                    return null;
                }
            }
        }
    }
}

sealed class Reference(open val target:String)
data class Xref(override val target:String, val anchor:String?) : Reference(target)
data class Anchor(override val target:String): Reference(target)


class ReferenceProviderImpl(private val basePath: Path, documents: Map<Path, PageHeader>) :
    ReferenceProvider {

    companion object {
        private val URI_DETECTOR = "^[a-zA-Z][a-zA-Z0-9.+-]+:/{0,2}".toRegex()
    }

    private val normalizedDocs = documents.map { (path, header) -> path.relativeTo(basePath).normalize() to header }.toMap()

    override fun resolveReference(source: Path, refTo: String): Reference? {
        if (URI_DETECTOR.matches(refTo)) return null;
        if (refTo.startsWith("#")) return Anchor(refTo.substring(1))

        val parts = refTo.split("#", limit =  2)
        val ref = parts[0]
        val anchor = parts.getOrNull(1)

        val targetPath = source.resolveSibling(ref).relativeTo(basePath).normalize()

        val document = normalizedDocs[targetPath]?.title ?: return null
        return Xref(document, anchor);
    }
}

