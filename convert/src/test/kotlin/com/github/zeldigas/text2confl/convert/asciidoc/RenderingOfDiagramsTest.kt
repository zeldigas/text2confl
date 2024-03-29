package com.github.zeldigas.text2confl.convert.asciidoc

import assertk.all
import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.startsWith
import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.AttachmentCollector
import com.github.zeldigas.text2confl.convert.AttachmentsRegistry
import com.github.zeldigas.text2confl.convert.PageHeader
import com.github.zeldigas.text2confl.convert.confluence.ReferenceProvider
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div

internal class RenderingOfDiagramsTest : RenderingTestBase() {

    @Test
    fun `Code block is replaced with image for registered asciidoc diagram generator`(@TempDir tempDir: Path) {
        val parser = AsciidocParser(
            AsciidoctorConfiguration(
                workdir = tempDir.resolve("out").also { it.createDirectories() },
                libsToLoad = listOf("asciidoctor-diagram"),
                loadBundledMacros = false,
                attributes = mapOf(
                    "outdir" to (tempDir / "out").toString(),
                    "imagesoutdir" to (tempDir / "out").toString()
                )
            )
        )
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
            attachmentsCollector = AsciidocAttachmentCollector(
                tempDir / "test.adoc", AttachmentCollector(
                    ReferenceProvider.fromDocuments(
                        tempDir, mapOf(
                            tempDir / "test.adoc" to PageHeader("Test", emptyMap())
                        )
                    ),
                    registry
                ),
                tempDir / "out"
            ),
            parser = parser
        )

        assertThat(registry.collectedAttachments).isEqualTo(
            mapOf(
                "auth-protocol.png" to Attachment.fromLink("auth-protocol.png", tempDir / "out" / "auth-protocol.png")
            )
        )
        assertThat(result).transform { it.replace("""ac:(height|width)="\d+"""".toRegex(), "ac:$1=\"?\"") }
            .isEqualToConfluenceFormat(
                """
            <p><ac:image ac:height="?" ac:width="?" ac:alt="auth protocol"><ri:attachment ri:filename="auth-protocol.png" /></ac:image></p>
            """.trimIndent()
            )
    }

    @Test
    fun `Code block is replaced with image for registered kroki diagram generator`(@TempDir tempDir: Path) {
        val parser = AsciidocParser(
            AsciidoctorConfiguration(
                workdir = tempDir.resolve("out").also { it.createDirectories() },
                libsToLoad = listOf("asciidoctor-kroki"),
                loadBundledMacros = false,
                attributes = mapOf(
                    "kroki-fetch-diagram" to "true",
                    "outdir" to (tempDir / "out").toString(),
                    "imagesoutdir" to (tempDir / "out").toString()
                )
            )
        )
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
            attachmentsCollector = AsciidocAttachmentCollector(
                tempDir / "test.adoc", AttachmentCollector(
                    ReferenceProvider.fromDocuments(
                        tempDir, mapOf(
                            tempDir / "test.adoc" to PageHeader("Test", emptyMap())
                        )
                    ),
                    registry
                ),
                tempDir / "out"
            ),
            parser = parser
        )

        println(result)

        assertThat(registry.collectedAttachments).hasSize(1)

        val (key, attachment) = registry.collectedAttachments.entries.first()
        assertThat(key).all {
            startsWith("auth-protocol")
            endsWith(".png")
        }
        assertThat(attachment).isEqualTo(
            Attachment.fromLink(key, tempDir / "out" / key)
        )
        assertThat(result).isEqualToConfluenceFormat(
            """
            <p><ac:image ac:alt="auth-protocol"><ri:attachment ri:filename="$key" /></ac:image></p>
            """.trimIndent()
        )
    }
}