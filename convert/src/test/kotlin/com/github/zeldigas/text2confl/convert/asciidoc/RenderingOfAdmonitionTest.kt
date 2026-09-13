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
    internal fun `Note admonition renders as ADF panel on cloud v2`() {
        val result = toHtml(
            """
            [NOTE]
            ====
            Test block **with** formatting
            ====
        """.trimIndent(),
            attributes = mapOf("t2c-editor-version" to "v2")
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ac:adf-extension><ac:adf-node type="panel"><ac:adf-attribute key="panel-type">note</ac:adf-attribute><ac:adf-content>
            <p>Test block <strong>with</strong> formatting</p>
            </ac:adf-content></ac:adf-node><ac:adf-fallback><div class="panel conf-macro output-block" style="background-color: rgb(234,230,255);border-color: rgb(153,141,217);border-width: 1.0px;"><div class="panelContent" style="background-color: rgb(234,230,255);">
            <p>Test block <strong>with</strong> formatting</p>
            </div></div></ac:adf-fallback></ac:adf-extension>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Note admonition with title on cloud v2 renders title as bold text`() {
        val result = toHtml(
            """
            [NOTE]
            .Title test
            ====
            Test
            ====
        """.trimIndent(),
            attributes = mapOf("t2c-editor-version" to "v2")
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ac:adf-extension><ac:adf-node type="panel"><ac:adf-attribute key="panel-type">note</ac:adf-attribute><ac:adf-content>
            <p><strong>Title test</strong></p><p>Test</p>
            </ac:adf-content></ac:adf-node><ac:adf-fallback><div class="panel conf-macro output-block" style="background-color: rgb(234,230,255);border-color: rgb(153,141,217);border-width: 1.0px;"><div class="panelContent" style="background-color: rgb(234,230,255);">
            <p><strong>Title test</strong></p><p>Test</p>
            </div></div></ac:adf-fallback></ac:adf-extension>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Callout admonition with title on cloud v2 renders title as bold text`() {
        val result = toHtml(
            """
            [TIP]
            .Title test
            ====
            Test
            ====
        """.trimIndent(),
            attributes = mapOf("t2c-editor-version" to "v2")
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ac:structured-macro ac:name="tip"><ac:rich-text-body>
            <p><strong>Title test</strong></p><p>Test</p>
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