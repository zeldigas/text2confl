package com.github.zeldigas.text2confl.convert.markdown

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.markdown.diagram.DiagramMaker
import com.github.zeldigas.text2confl.convert.markdown.diagram.DiagramMakersImpl
import com.github.zeldigas.text2confl.convert.markdown.diagram.GeneratedDiagram
import com.github.zeldigas.text2confl.convert.markdown.diagram.ImageInfo
import com.github.zeldigas.text2confl.convert.markdown.diagram.inlineMacros
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.nio.file.Path

internal class RenderingOfDiagramsTest : RenderingTestBase() {

    @Test
    fun `Code block is replaced with image for registered diagram generator`() {
        val diagramMaker = mockk<DiagramMaker> {
            every { toDiagram(any(), any(), any()) } returns GeneratedDiagram.Image(
                Attachment.fromLink("custom_name", Path.of("random_path")), ImageInfo(500, 600, "Diagram title")
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
        DiagramMakersImpl(Path.of("diagrams"), emptyList(), inlineMacros(mapOf(*macros)))

    @Test
    fun `Code block is replaced with inline content for configured format`() {
        val result = toHtml(
            """
            Text before

            ```mermaid
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

    @Test
    fun `Template variables and code block attributes are available in inline content`() {
        val template = """<macro tenant="{{tenantId}}" type="{{macroFormat}}" name="{{attributes.target}}"""" +
                """{{#attributes.width}} width="{{.}}"{{/attributes.width}}>{{{source}}}</macro>"""
        val result = toHtml(
            """
            ```mermaid {target="first & second" width=100}
            graph TD
            ```

            ```puml {target=third}
            A -> B
            ```
            """.trimIndent(),
            diagramMakers = inlineMakers(
                "mermaid" to DiagramMacroConfiguration(
                    listOf("mermaid"), template, mapOf("tenantId" to "abc", "macroFormat" to "mermaid")
                ),
                "plantuml" to DiagramMacroConfiguration(
                    listOf("plantuml", "puml"), template, mapOf("tenantId" to "abc", "macroFormat" to "plantuml")
                ),
            )
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <macro tenant="abc" type="mermaid" name="first &amp; second" width="100">graph TD</macro>
            <macro tenant="abc" type="plantuml" name="third">A -> B</macro>
            """.trimIndent()
        )
        assertThat(attachmentsRegistry.collectedAttachments).isEmpty()
    }
}
