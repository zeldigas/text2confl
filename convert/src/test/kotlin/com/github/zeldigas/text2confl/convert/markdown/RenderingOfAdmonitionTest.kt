package com.github.zeldigas.text2confl.convert.markdown

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.EditorVersion
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

internal class RenderingOfAdmonitionTest : RenderingTestBase() {

    @CsvSource(
        value = ["CAUTION,warning",
                "TIP,tip",
                "IMPORTANT,info",
                "WARNING,warning",
                "NOTE,note",
                ],
    )
    @ParameterizedTest
    internal fun `Github style admonitions`(ghType: String, parsedType: String){
        val result = toHtml(
            """
            > Regular block
            > quote
                
            > [!$ghType]
            > 
            > Test block **with** formatting
            > 1. and
            > 2. lists 
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <blockquote>
              <p>Regular block quote</p>
            </blockquote>
            <ac:structured-macro ac:name="$parsedType"><ac:rich-text-body>
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
    internal fun `Github style admonitions without new line`(){
        val result = toHtml(
            """
            > Regular block
            > quote
                
            > [!TIP]
            > Test block **with** formatting
            > 1. and
            > 2. lists 
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <blockquote>
              <p>Regular block quote</p>
            </blockquote>
            <ac:structured-macro ac:name="tip"><ac:rich-text-body>
            <p>Test block <strong>with</strong> formatting</p>
            <ol>
              <li>and</li>
              <li>lists</li>
            </ol>
            </ac:rich-text-body></ac:structured-macro>
        """.trimIndent()
        )
    }


    @ValueSource(strings = ["tip", "note", "warning", "info"])
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

    @ParameterizedTest
    @ValueSource(strings = ["v1", "v2"])
    internal fun `Expand admonition rendering is not affected by editor version`(editorVersion: String) {
        val result = toHtml(
            """
            !!! expand "Click to reveal"

                Test
        """.trimIndent(),
            editorVersion = if (editorVersion == "v2") EditorVersion.V2 else EditorVersion.V1
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ac:structured-macro ac:name="expand"><ac:parameter ac:name="title">Click to reveal</ac:parameter><ac:rich-text-body>
            <p>Test</p>
            </ac:rich-text-body></ac:structured-macro>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Note admonition renders as ADF panel on cloud v2`() {
        val result = toHtml(
            """
            !!! note

                Test block **with** formatting
        """.trimIndent(),
            editorVersion = EditorVersion.V2
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
            !!! note "Title test"

                Test
        """.trimIndent(),
            editorVersion = EditorVersion.V2
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ac:adf-extension><ac:adf-node type="panel"><ac:adf-attribute key="panel-type">note</ac:adf-attribute><ac:adf-content><p><strong>Title test</strong></p>
            <p>Test</p>
            </ac:adf-content></ac:adf-node><ac:adf-fallback><div class="panel conf-macro output-block" style="background-color: rgb(234,230,255);border-color: rgb(153,141,217);border-width: 1.0px;"><div class="panelContent" style="background-color: rgb(234,230,255);"><p><strong>Title test</strong></p>
            <p>Test</p>
            </div></div></ac:adf-fallback></ac:adf-extension>
        """.trimIndent()
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["tip", "warning", "info"])
    internal fun `Callout admonition with title on cloud v2 renders title as bold text`(type: String) {
        val result = toHtml(
            """
            !!! $type "Title test"

                Test
        """.trimIndent(),
            editorVersion = EditorVersion.V2
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ac:structured-macro ac:name="$type"><ac:rich-text-body><p><strong>Title test</strong></p>
            <p>Test</p>
            </ac:rich-text-body></ac:structured-macro>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Note admonition body is rendered consistently in both ADF content and fallback`() {
        val result = toHtml(
            """
            !!! note

                ![](assets/image.jpg)
        """.trimIndent(),
            attachments = mapOf(
                "assets/image.jpg" to Attachment(
                    "an_attachment",
                    "assets/image.jpg",
                    kotlin.io.path.Path("assets/image.jpg")
                )
            ),
            editorVersion = EditorVersion.V2
        )

        assertThat(result.split("ri:attachment").size - 1).isEqualTo(2)
    }

    @Test
    internal fun `Autogenerated banner renders as ADF panel on cloud v2`() {
        val result = toHtml(
            "Test",
            addAutogenHeader = true,
            autogenText = "Generated banner text",
            editorVersion = EditorVersion.V2
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p><ac:adf-extension><ac:adf-node type="panel"><ac:adf-attribute key="panel-type">note</ac:adf-attribute><ac:adf-content><p>Generated banner text</p></ac:adf-content></ac:adf-node><ac:adf-fallback><div class="panel conf-macro output-block" style="background-color: rgb(234,230,255);border-color: rgb(153,141,217);border-width: 1.0px;"><div class="panelContent" style="background-color: rgb(234,230,255);"><p>Generated banner text</p></div></div></ac:adf-fallback></ac:adf-extension></p>
            <p>Test</p>
        """.trimIndent()
        )
    }

}