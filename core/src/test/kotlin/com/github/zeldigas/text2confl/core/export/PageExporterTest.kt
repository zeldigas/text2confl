package com.github.zeldigas.text2confl.core.export

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.confclient.SimplePageLoadOptions
import com.github.zeldigas.confclient.model.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText

@ExtendWith(MockKExtension::class)
class PageExporterTest(
    @param:MockK val client: ConfluenceClient
) {

    val exporter = PageExporter(client, true)

    @Test
    fun `Export of page with attachments and labels`(@TempDir dir: Path) {
        coEvery { client.getPageById("abc", loadOptions = PageExporter.CONTENT_EXTENSIONS) } returns mockk {
            every { space?.key } returns "SP"
            every { title } returns "title"
            every { body?.storage?.value } returns "<p>hello world</p>"
            every { labels } returns listOf(
                Label("", "l1", "1"),
                Label("", "l2", "1")
            )
            every { attachments } returns mockk()
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

    @Test
    fun `Recursive export of page tree`(@TempDir dir: Path) {
        val space = Space(1, "SP", "Space", null, null)
        coEvery { client.getPageById("root", setOf(SimplePageLoadOptions.Space)) } returns
                ConfluencePage("root", "Root", space = space)
        coEvery { client.findChildPages("root") } returns listOf(
            ConfluencePage("a", "Child A"),
            ConfluencePage("b", "Child B"),
            ConfluencePage("c", "Child/B"),
        )
        coEvery { client.findChildPages("a") } returns emptyList()
        coEvery { client.findChildPages("c") } returns emptyList()
        coEvery { client.findChildPages("b") } returns listOf(ConfluencePage("g", "Grandchild"))
        coEvery { client.findChildPages("g") } returns emptyList()

        val rootAttachments = mockk<PageAttachments>()
        val childAttachments = mockk<PageAttachments>()
        mockContent("root", "Root", """<p>root</p>""", rootAttachments)
        mockContent(
            "a", "Child A",
            """<p><ac:link><ri:page ri:content-title="Child B"/></ac:link> and <ac:link><ri:page ri:content-title="External"/></ac:link></p>""",
            childAttachments
        )
        mockContent("b", "Child B", """<p>b</p>""")
        mockContent("c", "Child/B", """<p>c</p>""")
        mockContent(
            "g",
            "Grandchild",
            """<p><ac:link ac:anchor="sec"><ri:page ri:content-title="Root"/></ac:link></p>"""
        )

        coEvery { client.fetchAllAttachments(rootAttachments) } returns listOf(mockk { every { title } returns "my img.png" })
        coEvery { client.downloadAttachment(any(), dir / "_assets" / "Root" / "my img.png") } just Runs
        coEvery { client.fetchAllAttachments(childAttachments) } returns listOf(mockk { every { title } returns "a file.txt" })
        coEvery { client.downloadAttachment(any(), dir / "Root" / "_assets" / "Child A" / "a file.txt") } just Runs
        coEvery { client.getPage("SP", "External") } returns ConfluencePage(
            "ext", "External", links = mapOf("webui" to "/display/SP/External")
        )
        every { client.confluenceBaseUrl } returns io.ktor.http.Url("https://wiki.example.org/")

        val tracker = mockk<ExportProgressTracker>(relaxed = true)
        runBlocking { PageExporter(client, false).exportPageTree("root", dir, null, tracker) }

        verifyOrder {
            tracker.collectingTree("Root")
            tracker.treeCollected(5)
            tracker.pageExported(1, 5, "Root", dir / "Root.md")
            tracker.pageExported(2, 5, "Child A", dir / "Root" / "Child A.md")
            tracker.pageExported(3, 5, "Child B", dir / "Root" / "Child B.md")
            tracker.pageExported(4, 5, "Grandchild", dir / "Root" / "Child B" / "Grandchild.md")
            tracker.pageExported(5, 5, "Child/B", dir / "Root" / "Child_B.md")
            tracker.exportCompleted(5)
        }

        assertThat((dir / "Root.md").readText()).isEqualTo(
            """
            # Root
            
            root
            
            
            [my img.png]: _assets/Root/my%20img.png
        """.trimIndent()
        )
        assertThat((dir / "Root" / "Child A.md").readText()).isEqualTo(
            """
            # Child A
            
            [Child B](Child%20B.md) and [External](https://wiki.example.org/display/SP/External)
            
            
            [a file.txt]: _assets/Child%20A/a%20file.txt
        """.trimIndent()
        )
        assertThat((dir / "Root" / "Child B.md").readText()).isEqualTo("# Child B\n\nb\n")
        assertThat((dir / "Root" / "Child_B.md").readText()).isEqualTo("# Child/B\n\nc\n")
        assertThat((dir / "Root" / "Child B" / "Grandchild.md").readText()).isEqualTo(
            "# Grandchild\n\n[Root](../../Root.md#sec)\n"
        )
        assertThat((dir / "Root" / "_assets").listDirectoryEntries().map { it.name }).containsExactly("Child A")
        coVerify(exactly = 2) { client.downloadAttachment(any(), any()) }
    }

    @Test
    fun `Sibling pages with same sanitized name get unique files`(@TempDir dir: Path) {
        val space = Space(1, "SP", "Space", null, null)
        coEvery { client.getPage("SP", "Root", setOf(SimplePageLoadOptions.Space)) } returns
                ConfluencePage("root", "Root", space = space)
        coEvery { client.findChildPages("root") } returns listOf(
            ConfluencePage("a", "A/B"),
            ConfluencePage("b", "A_B"),
        )
        coEvery { client.findChildPages(match { it != "root" }) } returns emptyList()
        mockContent("root", "Root", "<p>r</p>")
        mockContent("a", "A/B", "<p>a</p>")
        mockContent("b", "A_B", "<p>b</p>")

        runBlocking { PageExporter(client, false).exportPageTree("SP", "Root", dir, "attachments") }

        assertThat((dir / "Root" / "A_B.md").readText()).isEqualTo("# A/B\n\na\n")
        assertThat((dir / "Root" / "A_B_2.md").readText()).isEqualTo("# A_B\n\nb\n")
    }

    private fun mockContent(id: String, title: String, body: String, attachments: PageAttachments? = null) {
        coEvery { client.getPageById(id, PageExporter.CONTENT_EXTENSIONS) } returns ConfluencePage(
            id, title,
            labels = emptyList(),
            attachments = attachments,
            body = PageBody(StorageFormat(body, "storage")),
            space = Space(1, "SP", "Space", null, null)
        )
    }
}
