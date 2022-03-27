package com.github.zeldigas.text2confl.convert.asciidoc

import assertk.Assert
import assertk.assertions.isEqualTo
import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.confluence.LanguageMapper
import com.github.zeldigas.text2confl.convert.confluence.LanguageMapperImpl
import com.github.zeldigas.text2confl.convert.confluence.ReferenceProvider

internal open class RenderingTestBase  {

    val parser: AsciidocParser = AsciidocParser()

    val languageMapper: LanguageMapper = LanguageMapperImpl(setOf("java", "kotlin"), "fallback")

    fun <T> Assert<T>.isEqualToConfluenceFormat(expected: String) {
        isEqualTo(expected)
    }

    fun toHtml(
        src: String,
        attachments: Map<String, Attachment> = emptyMap(),
        referenceProvider: ReferenceProvider = ReferenceProvider.singleFile(),
        languageMapper: LanguageMapper? = null,
        addAutogenHeader:Boolean = false,
        autogenText:String = "Generated for __doc-root____file__"
    ): String {
        val ast = parser.parseDocument(src, AsciidocRenderingParameters(
            languageMapper ?: this.languageMapper,
            referenceProvider,
            autogenText,
            addAutogenHeader,
            "TEST"
        ))

        return ast.convert()
    }

}