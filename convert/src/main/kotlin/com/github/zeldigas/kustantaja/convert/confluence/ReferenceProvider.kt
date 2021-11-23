package com.github.zeldigas.kustantaja.convert.confluence

import com.github.zeldigas.kustantaja.convert.PageHeader
import java.nio.file.Path

interface ReferenceProvider {
    fun resolveReference(source: Path, refTo: String): PageHeader?

    companion object {
        fun fromDocuments(basePath: Path, documents: Map<Path, PageHeader>): ReferenceProvider {
            return ReferenceProviderImpl(basePath, documents)
        }

        fun nop(): ReferenceProvider {
            return object : ReferenceProvider {
                override fun resolveReference(source: Path, refTo: String): PageHeader? {
                    return null;
                }
            }
        }
    }
}

class ReferenceProviderImpl(private val basePath: Path, private val documents: Map<Path, PageHeader>) :
    ReferenceProvider {
    override fun resolveReference(source: Path, refTo: String): PageHeader? {
        return null;
    }
}

