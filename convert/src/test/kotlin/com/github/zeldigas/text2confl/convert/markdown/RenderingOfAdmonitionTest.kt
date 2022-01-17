package com.github.zeldigas.text2confl.convert.markdown

import assertk.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class RenderingOfAdmonitionTest : RenderingTestBase() {

    @ValueSource(strings = ["tip", "note", "warning", "info", "expand"])
    @ParameterizedTest
    internal fun `Confluence supported admonitions`(type: String) {
        val result = toHtml(
            """
            !!! $type
            
                Test block **with** formatting
                1. and
                2. lists
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ac:structured-macro ac:name="$type"><ac:rich-text-body>
            <p>Test block <strong>with</strong> formatting</p>
            <ol>
              <li>and</li>
              <li>lists</li>
            </ol>
            </ac:rich-text-body></ac:structured-macro>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Confluence unsupported admonitions is rendered as note`() {
        val result = toHtml(
            """
            !!! bug "Title test"
            
                Test
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ac:structured-macro ac:name="note"><ac:parameter ac:name="title">Title test</ac:parameter><ac:rich-text-body>
            <p>Test</p>
            </ac:rich-text-body></ac:structured-macro>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Collapsed admonition is rendered as simple ones`() {
        val result = toHtml(
            """
            ??? info
            
                Test
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ac:structured-macro ac:name="info"><ac:rich-text-body>
            <p>Test</p>
            </ac:rich-text-body></ac:structured-macro>
        """.trimIndent()
        )
    }


}