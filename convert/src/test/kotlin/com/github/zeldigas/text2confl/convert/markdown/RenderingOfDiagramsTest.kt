package com.github.zeldigas.text2confl.convert.markdown

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.markdown.diagram.DiagramMaker
import com.github.zeldigas.text2confl.convert.markdown.diagram.ImageInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.nio.file.Path

internal class RenderingOfDiagramsTest : RenderingTestBase() {

    @Test
    fun `Code block is replaced with image for registered diagram generator`() {
        val diagramMaker = mockk<DiagramMaker> {
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
            <p><ac:image ac:height="500" ac:width="600" ac:title="Diagram title"><ri:attachment ri:filename="custom_name" /></ac:image></p>
            """.trimIndent()
        )
        assertThat(attachmentsRegistry.collectedAttachments).isEqualTo(
            mapOf(
                "random_path" to Attachment.fromLink("custom_name", Path.of("random_path"))
            )
        )
    }
}