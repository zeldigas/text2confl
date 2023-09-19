package com.github.zeldigas.text2confl.convert.asciidoc

import assertk.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class RenderingOfAdmonitionTest : RenderingTestBase() {

    @CsvSource(value = ["TIP,tip", "NOTE,info", "CAUTION,note", "WARNING,warning", "IMPORTANT,warning"])
    @ParameterizedTest
    internal fun `Confluence supported admonitions`(type: String, confluenceType: String) {
        val result = toHtml(
            """
            [$type]
            ====
            Test block **with** formatting
            and line breaks
            in paragraph
            
            1. and
            2. lists
            ====
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ac:structured-macro ac:name="$confluenceType"><ac:rich-text-body>
            <p>Test block <strong>with</strong> formatting and line breaks in paragraph</p>
            <ol><li>and</li><li>lists</li></ol>
            </ac:rich-text-body></ac:structured-macro>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Admonition with title`() {
        val result = toHtml(
            """
            [NOTE]
            .Title test
            ====
            Test
            ====
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ac:structured-macro ac:name="info"><ac:parameter ac:name="title">Title test</ac:parameter><ac:rich-text-body>
            <p>Test</p>
            </ac:rich-text-body></ac:structured-macro>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Expand blocks rendering`() {
        val result = toHtml(
            """
            .Click to reveal the answer
            [%collapsible]
            ====
            Test
            ====
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ac:structured-macro ac:name="expand"><ac:parameter ac:name="title">Click to reveal the answer</ac:parameter><ac:rich-text-body>
            <p>Test</p>
            </ac:rich-text-body></ac:structured-macro>
        """.trimIndent()
        )
    }


}