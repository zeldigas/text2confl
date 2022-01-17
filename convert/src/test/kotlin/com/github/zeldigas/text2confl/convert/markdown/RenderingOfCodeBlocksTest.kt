package com.github.zeldigas.text2confl.convert.markdown

import assertk.Assert
import assertk.assertThat
import com.github.zeldigas.text2confl.convert.confluence.LanguageMapper
import org.junit.jupiter.api.Test

internal class RenderingOfCodeBlocksTest : RenderingTestBase() {


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
    internal fun `Fenced code block with attributes`() {
        fun Assert<String>.codeBlockWithAttributes() {
            isEqualToConfluenceFormat(
                """<ac:structured-macro ac:name="code"><ac:parameter ac:name="language">kotlin</ac:parameter>"""
                        +"""<ac:parameter ac:name="title">hello.kt &amp; world</ac:parameter>"""
                        +"""<ac:parameter ac:name="collapse">true</ac:parameter>"""
                        +"""<ac:parameter ac:name="linenumbers">true</ac:parameter>"""
                        +"""<ac:parameter ac:name="firstline">3</ac:parameter>"""
                        +"""<ac:parameter ac:name="theme">Eclipse</ac:parameter>"""
                        +"""<ac:plain-text-body><![CDATA[println("Hello")]]></ac:plain-text-body></ac:structured-macro>"""
            )
        }

        val attributes = """{title="hello.kt & world" collapse=true linenumbers=true firstline=3 theme=Eclipse unknown=yes}"""
        val result = toHtml(
            """
            ```kotlin $attributes
            println("Hello")
            ```
            """.trimIndent()
        )
        assertThat(result).codeBlockWithAttributes()

        val result1 = toHtml(
            """
            ```kotlin 
            println("Hello")
            ```
            $attributes
            """.trimIndent()
        )
        assertThat(result1).codeBlockWithAttributes()
    }
}


