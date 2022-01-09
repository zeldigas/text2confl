package com.github.zeldigas.text2confl.convert.markdown

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import com.github.zeldigas.text2confl.convert.*
import com.github.zeldigas.text2confl.convert.confluence.LanguageMapper
import com.github.zeldigas.text2confl.convert.confluence.ReferenceProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

internal class MarkdownFileConverterTest {

    private val converter = MarkdownFileConverter()

    @Test
    internal fun `Read header with no title frontmatter`(@TempDir dir: Path) {
        val file = dir.resolve("src.md")
        file.writeText("""
            Some markdown
        """.trimIndent())

        val result = converter.readHeader(file, HeaderReadingContext(titleTransformer()))

        assertThat(result).isEqualTo(PageHeader("Prefixed: src", emptyMap()))
    }

    @Test
    internal fun `Read header with title frontmatter`(@TempDir dir: Path) {
        val file = dir.resolve("src.md")
        file.writeText("""
            ---
            title: Custom title
            labels:
             - one
             - two
            ---
            
            Some markdown
        """.trimIndent())

        val result = converter.readHeader(file, HeaderReadingContext(titleTransformer()))

        assertThat(result).isEqualTo(PageHeader("Prefixed: Custom title", mapOf(
            "title" to "Custom title",
            "labels" to listOf("one", "two")
        )))
    }

    private fun titleTransformer(): (Path, String) -> String = { _, title -> "Prefixed: $title" }

    @Test
    internal fun `Convert file`(@TempDir dir: Path) {
        val file = dir.resolve("src.md")
        file.writeText("""            
            Some markdown [with attachment](test.txt)
            
            Code block:
            ```java
            System.out.println("Hello world!");
            ```
            
            ## Second level section
            
            [Link](https://example.org)
        """.trimIndent())
        Files.createFile(dir.resolve("test.txt"))

        val result = converter.convert(file, ConvertingContext(
            ReferenceProvider.nop(), LanguageMapper.forServer(), "TEST",
            titleTransformer()
        ))

        assertThat(result).all {
            prop(PageContent::header).isEqualTo(PageHeader("Prefixed: src", emptyMap()))
            prop(PageContent::attachments).isEqualTo(
                listOf(Attachment("test.txt", "test.txt", dir.resolve("test.txt")))
            )
            prop(PageContent::body).isEqualTo("""
            <p>Some markdown <ac:link><ri:attachment ri:filename="test.txt" /><ac:plain-text-link-body><![CDATA[with attachment]]></ac:plain-text-link-body></ac:link></p>
            <p>Code block:</p>
            <ac:structured-macro ac:name="code"><ac:parameter ac:name="language">java</ac:parameter><ac:plain-text-body><![CDATA[System.out.println("Hello world!");]]></ac:plain-text-body></ac:structured-macro>
            <h2>Second level section<ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">second-level-section</ac:parameter></ac:structured-macro></h2>
            
            <p><a href="https://example.org">Link</a></p>
            """.trimIndent() + "\n\n")
        }
    }
}