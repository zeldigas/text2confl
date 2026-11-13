package com.github.zeldigas.text2confl.convert.markdown

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.markdown.diagram.DiagramMakersImpl
import com.github.zeldigas.text2confl.convert.markdown.diagram.ImageDiagramMaker
import com.github.zeldigas.text2confl.convert.markdown.diagram.ImageInfo
import com.github.zeldigas.text2confl.convert.markdown.diagram.inlineTemplates
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.nio.file.Path

internal class RenderingOfDiagramsTest : RenderingTestBase() {

    @Test
    fun `Code block is replaced with image for registered diagram generator`() {
        val diagramMaker = mockk<ImageDiagramMaker> {
            every { toDiagram(any(), any(), any()) } returns (
                    Attachment.fromLink("custom_name", Path.of("random_path")) to ImageInfo(500, 600, "Diagram title")
                    )
        }
        val result = toHtml(
            """
            ```test-diagram {title="Diagram title"}
            test diagram
            script: A -> B
            ```
            """.trimIndent(),
            diagramMakers = mockk {
                every { find("test-diagram") } returns diagramMaker
            }
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p><ac:image ac:title="Diagram title" ac:height="500" ac:width="600"><ri:attachment ri:filename="custom_name" /></ac:image></p>
            """.trimIndent()
        )
        assertThat(attachmentsRegistry.collectedAttachments).isEqualTo(
            mapOf(
                "random_path" to Attachment.fromLink("custom_name", Path.of("random_path"))
            )
        )
    }

    private fun inlineMakers(vararg macros: Pair<String, DiagramMacroConfiguration>) =
        DiagramMakersImpl(Path.of("diagrams"), emptyList(), inlineTemplates(mapOf(*macros)))

    @Test
    fun `Code block is replaced with inline content for configured format`() {
        val result = toHtml(
            """
            Text before

            ```mermaid {target=ignored}
            graph TD

              A["Quoted & <tagged>"] --> B[中文]
            ```

            Text after
            """.trimIndent(),
            diagramMakers = inlineMakers(
                "diagram" to DiagramMacroConfiguration(
                    formats = listOf("mermaid"),
                    template = """<ac:structured-macro ac:name="diagram"><ac:parameter ac:name="input">{{lang}}</ac:parameter><ac:plain-text-body>{{#cdata}}{{{source}}}{{/cdata}}</ac:plain-text-body></ac:structured-macro>"""
                )
            )
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p>Text before</p>
            <ac:structured-macro ac:name="diagram"><ac:parameter ac:name="input">mermaid</ac:parameter><ac:plain-text-body><![CDATA[graph TD

              A["Quoted & <tagged>"] --> B[中文]]]></ac:plain-text-body></ac:structured-macro>
            <p>Text after</p>
            """.trimIndent()
        )
        assertThat(attachmentsRegistry.collectedAttachments).isEmpty()
    }

    @Test
    fun `Inline content is rendered inside other blocks`() {
        val result = toHtml(
            """
            * item

              ```mermaid
              graph TD
                A --> B
              ```
            """.trimIndent(),
            diagramMakers = inlineMakers(
                "diagram" to DiagramMacroConfiguration(listOf("mermaid"), "<macro>\n{{#cdata}}{{{source}}}{{/cdata}}\n</macro>")
            )
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ul>
              <li>
                <p>item</p>
                <macro>
            <![CDATA[graph TD
              A --> B]]>
            </macro>
              </li>
            </ul>
            """.trimIndent()
        )
    }
}
