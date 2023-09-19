package com.github.zeldigas.text2confl.core.export

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.confclient.model.Label
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.readText

@ExtendWith(MockKExtension::class)
class PageExporterTest(
    @MockK val client: ConfluenceClient
) {

    val exporter = PageExporter(client, true)

    @Test
    fun `Export of page with attachments and labels`(@TempDir dir: Path) {
        coEvery { client.getPageById("abc", expansions = PageExporter.CONTENT_EXTENSIONS) } returns mockk {
            every { space?.key } returns "SP"
            every { title } returns "title"
            every { body?.storage?.value } returns "<p>hello world</p>"
            every { metadata?.labels?.results } returns listOf(
                Label("", "l1", "1"),
                Label("", "l2", "1")
            )
            every { children?.attachment } returns mockk()
        }
        coEvery { client.fetchAllAttachments(any()) } returns listOf(
            mockk {
                every { title } returns "attach"
            },
            mockk {
                every { title } returns "attach1"
            })

        coEvery { client.downloadAttachment(any(), dir / "_assets" / "attach") } just Runs
        coEvery { client.downloadAttachment(any(), dir / "_assets" / "attach1") } just Runs

        runBlocking { exporter.exportPageContent("abc", dir, "_assets") }

        assertThat((dir / "title.md").readText()).isEqualTo(
            """
            ---
            labels: l1, l2
            ---
            
            # title
            
            hello world
            
            
            [attach]: _assets/attach
            
            [attach1]: _assets/attach1
        """.trimIndent()
        )
    }
}