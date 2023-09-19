package com.github.zeldigas.text2confl.convert.markdown

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.AttachmentsRegistry
import com.github.zeldigas.text2confl.convert.confluence.Anchor
import com.github.zeldigas.text2confl.convert.confluence.ReferenceProvider
import com.github.zeldigas.text2confl.convert.confluence.Xref
import com.vladsch.flexmark.parser.Parser
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

@ExtendWith(MockKExtension::class)
internal class MarkdownAttachmentCollectorTest {

    private val parser = Parser.builder().build()
    private val registry = AttachmentsRegistry()

    @Test
    internal fun `Attachment collection for links`(@TempDir dir: Path, @MockK referenceProvider: ReferenceProvider) {
        Files.createFile(dir.resolve("existing"))
        Files.createFile(dir.resolve("existing1"))
        Files.createFile(dir.resolve("additional"))

        val ast = parser.parse(
            """
            Link to [existing file](existing)
            Link to [exsting doc](existing.md)
            Link to [anchor](#anchor)
            Link to [non-existing file](non-existing)
            Link to [smth][test]
            Link to [smth-missing][ref2]
            Link to [remote](http://example.org)
            some text
            
            [test]: existing1 "title"
            [ref2]: non-existing "title"
            
            [additional-attachment]: additional
            
        """.trimIndent()
        )

        val doc = dir.resolve("doc.md")

        every { referenceProvider.resolveReference(doc, any()) } returns null
        every { referenceProvider.resolveReference(doc, "existing.md") } returns Xref("test", null)
        every { referenceProvider.resolveReference(doc, "anchor") } returns Anchor("anchor")

        MarkdownAttachmentCollector(doc, referenceProvider, registry).collectAttachments(ast)

        assertThat(registry.collectedAttachments).isEqualTo(
            mapOf(
                "existing" to Attachment.fromLink("existing", dir.resolve("existing")),
                "test" to Attachment.fromLink("test", dir.resolve("existing1")),
                "additional-attachment" to Attachment.fromLink("additional-attachment", dir.resolve("additional")),
            )
        )
    }

    @Test
    internal fun `Attachment collection for empty links`(
        @TempDir dir: Path,
        @MockK referenceProvider: ReferenceProvider
    ) {
        val ast = parser.parse(
            """
            Link to [empty link]()
            Link to [smth][test]            
            [dangling]
            
            [test]: 
            
        """.trimIndent()
        )

        val doc = dir.resolve("doc.md")

        MarkdownAttachmentCollector(doc, referenceProvider, registry).collectAttachments(ast)

        assertThat(registry.collectedAttachments).isEmpty()
    }

    @Test
    internal fun `Attachment collection for images`(@TempDir dir: Path, @MockK referenceProvider: ReferenceProvider) {
        Files.createFile(dir.resolve("existing"))
        Files.createFile(dir.resolve("existing1"))

        val ast = parser.parse(
            """
            Link to ![existing file](existing)
            Link to ![non-existing file](non-existing)
            Link to ![smth][test]
            Link to ![smth][ref2]
            Link to ![remote](http://example.org/test.jpg)
            some text
            
            [test]: existing1 "title"
            [ref2]: non-existing "title"
            
        """.trimIndent()
        )

        val doc = dir.resolve("doc.md")

        every { referenceProvider.resolveReference(doc, any()) } returns null

        MarkdownAttachmentCollector(doc, referenceProvider, registry).collectAttachments(ast)

        assertThat(registry.collectedAttachments).isEqualTo(
            mapOf(
                "existing" to Attachment.fromLink("existing", dir.resolve("existing")),
                "test" to Attachment.fromLink("test", dir.resolve("existing1"))
            )
        )
    }

    @Test
    internal fun `Attachment collection for empty image links`(
        @TempDir dir: Path,
        @MockK referenceProvider: ReferenceProvider
    ) {
        val ast = parser.parse(
            """
            Link to ![empty link]()
            Link to ![smth][test]
            ![dangling]
            
            [test]: 
            
        """.trimIndent()
        )

        val doc = dir.resolve("doc.md")

        MarkdownAttachmentCollector(doc, referenceProvider, registry).collectAttachments(ast)

        assertThat(registry.collectedAttachments).isEmpty()
    }
}