package com.github.zeldigas.text2confl.convert.asciidoc

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.AttachmentsRegistry
import org.junit.jupiter.api.Test
import java.nio.file.Path

internal class RenderingOfDiagramsTest : RenderingTestBase() {

    @Test
    fun `Code block is replaced with image for registered diagram generator`() {
        val registry = AttachmentsRegistry()
        val result = toHtml(
            """
            [plantuml,auth-protocol,png]
            ....
            Alice -> Bob: Authentication Request
            Bob --> Alice: Authentication Response
            
            Alice -> Bob: Another authentication Request
            Alice <-- Bob: another authentication Response
            ....
            """.trimIndent(),
            attachmentsRegistry = registry
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p><ac:image ac:title="Diagram title" ac:height="500" ac:width="600"><ri:attachment ri:filename="auth-protocol.png" /></ac:image></p>
            """.trimIndent()
        )
        assertThat(registry.collectedAttachments).isEqualTo(
            mapOf(
                "auth-protocol.png" to Attachment.fromLink("auth-protocol.png", Path.of("auth-protocol.png"))
            )
        )
    }
}