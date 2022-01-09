package com.github.zeldigas.text2confl.convert.markdown

import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.ConvertingContext
import com.github.zeldigas.text2confl.convert.confluence.LanguageMapper
import com.github.zeldigas.text2confl.convert.confluence.LanguageMapperImpl
import com.github.zeldigas.text2confl.convert.confluence.ReferenceProvider
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

internal class ConfluenceNodeRendererTest {
    private val parser: MarkdownParser = MarkdownParser()

    private val languageMapper: LanguageMapper = LanguageMapperImpl(setOf("java", "kotlin"), "fallback")

    @Test
    internal fun `Fenced code block with language tag rendering`() {
        val result = toHtml(
            """
            ```kotlin
            println("Hello")
            println("world")
            ```
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ac:structured-macro ac:name="code"><ac:parameter ac:name="language">kotlin</ac:parameter><ac:plain-text-body><![CDATA[println("Hello")
            println("world")]]></ac:plain-text-body></ac:structured-macro>
        """.trimIndent()
        )

        val resultWithUndefinedLang = toHtml(
            """
            ```ruby
            puts "Hello"           
            ```
        """.trimIndent()
        )

        assertThat(resultWithUndefinedLang).isEqualToConfluenceFormat(
            """
            <ac:structured-macro ac:name="code"><ac:parameter ac:name="language">fallback</ac:parameter><ac:plain-text-body><![CDATA[puts "Hello"]]></ac:plain-text-body></ac:structured-macro>
        """.trimIndent()
        )

        val resultWithNullLang = toHtml(
            """
            ```ruby
            puts "Hello"           
            ```
        """.trimIndent(), languageMapper = LanguageMapper.nop()
        )

        assertThat(resultWithNullLang).isEqualToConfluenceFormat(
            """
            <ac:structured-macro ac:name="code"><ac:plain-text-body><![CDATA[puts "Hello"]]></ac:plain-text-body></ac:structured-macro>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Fenced code block without language tag rendering`() {
        val result = toHtml(
            """
            ```
            A text in code
            block
            ```
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ac:structured-macro ac:name="code"><ac:plain-text-body><![CDATA[A text in code
            block]]></ac:plain-text-body></ac:structured-macro>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Simple code block is rendered as pretext`() {
        val result = toHtml(
            """
            |    Text that will be rendered as pre
            |    code
        """.trimMargin("|")
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <pre><code>Text that will be rendered as pre
            code
            </code></pre>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Headings rendering`() {
        val result = toHtml(
            """
            # First header
            
            Some paragraph
            
            ## Subheader
            
            Par inside
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <h1>First header<ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">first-header</ac:parameter></ac:structured-macro></h1>
            <p>Some paragraph</p>
            <h2>Subheader<ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">subheader</ac:parameter></ac:structured-macro></h2>
            <p>Par inside</p>
        """.trimIndent()
        )
    }

    private fun toHtml(
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

private fun <T> Assert<T>.isEqualToConfluenceFormat(expected: String) {
    isEqualTo(expected + "\n")
}
