package com.github.zeldigas.text2confl.convert.asciidoc

import com.github.zeldigas.text2confl.convert.confluence.Anchor
import com.github.zeldigas.text2confl.convert.confluence.Reference
import com.github.zeldigas.text2confl.convert.confluence.ReferenceProvider
import com.github.zeldigas.text2confl.convert.confluence.Xref
import java.nio.file.Path

class AsciidocReferenceProvider(
    private val source: Path,
    internal val referenceProvider: ReferenceProvider
) {

    fun resolveXref(target: String): AsciidocRef? {
        val ref = referenceProvider.resolveReference(source, target.replace(".html", ".adoc")) ?: return null
        return toAsciidocRef(ref)
    }

    fun resolveLink(target: String): AsciidocRef? {
        val ref = referenceProvider.resolveReference(source, target) ?: return null
        return toAsciidocRef(ref)
    }

    private fun toAsciidocRef(ref: Reference): AsciidocRef {
        return when (ref) {
            is Xref -> AsciidocRef(ref.target, ref.anchor)
            is Anchor -> AsciidocRef(null, ref.target)
        }
    }

}

data class AsciidocRef(val page: String?, val anchor: String?)