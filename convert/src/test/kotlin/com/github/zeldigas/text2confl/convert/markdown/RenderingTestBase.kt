package com.github.zeldigas.text2confl.convert.markdown

import assertk.Assert
import assertk.assertions.isEqualTo
import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.ConvertingContext
import com.github.zeldigas.text2confl.convert.confluence.LanguageMapper
import com.github.zeldigas.text2confl.convert.confluence.LanguageMapperImpl
import com.github.zeldigas.text2confl.convert.confluence.ReferenceProvider
import kotlin.io.path.Path

internal open class RenderingTestBase  {

    val parser: MarkdownParser = MarkdownParser()

    val languageMapper: LanguageMapper = LanguageMapperImpl(setOf("java", "kotlin"), "fallback")

    fun <T> Assert<T>.isEqualToConfluenceFormat(expected: String) {
        isEqualTo(expected + "\n")
    }

    fun toHtml(
        src: String,
        attachments: Map<String, Attachment> = emptyMap(),
        referenceProvider: ReferenceProvider = ReferenceProvider.nop(),
        languageMapper: LanguageMapper? = null
    ): String {
        val ast = parser.parseString(src)

        val htmlRenderer = parser.htmlRenderer(
            Path("src.md"), attachments, ConvertingContext(
                referenceProvider, languageMapper ?: this.languageMapper, "TEST", { _, title -> title }
            )
        )

        return htmlRenderer.render(ast)
    }

}