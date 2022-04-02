package com.github.zeldigas.text2confl.convert.markdown

import assertk.Assert
import assertk.assertions.isEqualTo
import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.ConversionParameters
import com.github.zeldigas.text2confl.convert.ConvertingContext
import com.github.zeldigas.text2confl.convert.confluence.LanguageMapper
import com.github.zeldigas.text2confl.convert.confluence.LanguageMapperImpl
import com.github.zeldigas.text2confl.convert.confluence.ReferenceProvider
import kotlin.io.path.Path

internal open class RenderingTestBase {

    val languageMapper: LanguageMapper = LanguageMapperImpl(setOf("java", "kotlin"), "fallback")

    fun <T> Assert<T>.isEqualToConfluenceFormat(expected: String) {
        isEqualTo(expected + "\n")
    }

    fun toHtml(
        src: String,
        attachments: Map<String, Attachment> = emptyMap(),
        referenceProvider: ReferenceProvider = ReferenceProvider.singleFile(),
        languageMapper: LanguageMapper? = null,
        addAutogenHeader: Boolean = false,
        autogenText: String = "Generated for __doc-root____file__",
        config: MarkdownConfiguration = MarkdownConfiguration(true, emptyList())
    ): String {
        val parser = MarkdownParser(config)
        val ast = parser.parseString(src)

        val htmlRenderer = parser.htmlRenderer(
            Path("src.md"), attachments, ConvertingContext(
                referenceProvider,
                ConversionParameters(
                    languageMapper ?: this.languageMapper,
                    { _, title -> title },
                    addAutogenHeader,
                    noteText = autogenText,
                    docRootLocation = "http://example.com/",
                    markdownConfiguration = config
                ),
                "TEST",
            )
        )

        return htmlRenderer.render(ast)
    }

}