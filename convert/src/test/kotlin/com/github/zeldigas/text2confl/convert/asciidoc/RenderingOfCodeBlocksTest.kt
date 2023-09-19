package com.github.zeldigas.text2confl.convert.asciidoc

import assertk.Assert
import assertk.assertThat
import com.github.zeldigas.text2confl.convert.confluence.LanguageMapper
import org.junit.jupiter.api.Test

internal class RenderingOfCodeBlocksTest : RenderingTestBase() {


    @Test
    internal fun `Source code block with language tag rendering`() {
        val result = toHtml(
            """
            [source,kotlin]
            ----
            println("Hello")
            println("world")
            println(if (a && b > 0 || c<0) "a" else "b")
            ----
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ac:structured-macro ac:name="code"><ac:parameter ac:name="language">kotlin</ac:parameter><ac:plain-text-body><![CDATA[println("Hello")
            println("world")
            println(if (a && b > 0 || c<0) "a" else "b")]]></ac:plain-text-body></ac:structured-macro>
        """.trimIndent()
        )

        val resultWithUndefinedLang = toHtml(
            """
            [source,ruby]
            ----
            puts "Hello"           
            ----
        """.trimIndent()
        )

        assertThat(resultWithUndefinedLang).isEqualToConfluenceFormat(
            """
            <ac:structured-macro ac:name="code"><ac:parameter ac:name="language">fallback</ac:parameter><ac:plain-text-body><![CDATA[puts "Hello"]]></ac:plain-text-body></ac:structured-macro>
        """.trimIndent()
        )

        val resultWithNullLang = toHtml(
            """
            [source,ruby]
            ----
            puts "Hello"           
            ----
        """.trimIndent(), languageMapper = LanguageMapper.nop()
        )

        assertThat(resultWithNullLang).isEqualToConfluenceFormat(
            """
            <ac:structured-macro ac:name="code"><ac:plain-text-body><![CDATA[puts "Hello"]]></ac:plain-text-body></ac:structured-macro>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Missed block type is supported`() {
        val result = toHtml(
            """
            [,kotlin]
            ----
            println("Hello")
            println("world")
            ----
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ac:structured-macro ac:name="code"><ac:parameter ac:name="language">kotlin</ac:parameter><ac:plain-text-body><![CDATA[println("Hello")
            println("world")]]></ac:plain-text-body></ac:structured-macro>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Source code block without language tag rendering`() {
        val result = toHtml(
            """
            [source]
            ----
            A text in code
            block
            ----
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
    internal fun `Source code block with asciidoc attributes converted to confluence analogs`() {
        fun Assert<String>.codeBlockWithAttributes() {
            isEqualToConfluenceFormat(
                """<ac:structured-macro ac:name="code"><ac:parameter ac:name="language">kotlin</ac:parameter>"""
                        + """<ac:parameter ac:name="title">hello.kt &amp; world</ac:parameter>"""
                        + """<ac:parameter ac:name="collapse">true</ac:parameter>"""
                        + """<ac:parameter ac:name="linenumbers">true</ac:parameter>"""
                        + """<ac:parameter ac:name="firstline">3</ac:parameter>"""
                        + """<ac:parameter ac:name="theme">Eclipse</ac:parameter>"""
                        + """<ac:plain-text-body><![CDATA[println("Hello")]]></ac:plain-text-body></ac:structured-macro>"""
            )
        }

        val result = toHtml(
            """
            [,kotlin,collapse,linenums,start=3,collapse=true,theme=Eclipse,title="hello.kt & world"]                
            ----
            println("Hello")
            ----
            """.trimIndent()
        )
        assertThat(result).codeBlockWithAttributes()
    }

    @Test
    internal fun `Listing block is rendered as pretext`() {
        val result = toHtml(
            """
        [listing]
        This is an example of a paragraph assigned
        the `listing` style in an attribute list.
        Notice that the monospace marks are
        preserved in the output.
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ac:structured-macro ac:name="noformat"><ac:plain-text-body><![CDATA[This is an example of a paragraph assigned
            the `listing` style in an attribute list.
            Notice that the monospace marks are
            preserved in the output.]]></ac:plain-text-body></ac:structured-macro>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Delimited listing block is rendered as pretext`() {
        val result = toHtml(
            """
            ----
            Text that will be rendered as <pre>
            and no code
            ----
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ac:structured-macro ac:name="noformat"><ac:plain-text-body><![CDATA[Text that will be rendered as <pre>
            and no code]]></ac:plain-text-body></ac:structured-macro>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Literal paragraph`() {
        val result = toHtml(
            """
            [literal]
            error: 1954 Forbidden search
            absolutely fatal: operation lost in the dodecahedron of doom
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ac:structured-macro ac:name="noformat"><ac:plain-text-body><![CDATA[error: 1954 Forbidden search
            absolutely fatal: operation lost in the dodecahedron of doom]]></ac:plain-text-body></ac:structured-macro>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Delimited literal block`() {
        val result = toHtml(
            """
            ....
            Kismet: Where is the *defensive operations manual*?

            Computer: Calculating ...
            ....
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ac:structured-macro ac:name="noformat"><ac:plain-text-body><![CDATA[Kismet: Where is the *defensive operations manual*?

            Computer: Calculating ...]]></ac:plain-text-body></ac:structured-macro>
        """.trimIndent()
        )
    }
}


