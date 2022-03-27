package com.github.zeldigas.text2confl.convert.asciidoc

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import com.github.zeldigas.text2confl.convert.*
import com.github.zeldigas.text2confl.convert.confluence.LanguageMapper
import com.github.zeldigas.text2confl.convert.confluence.ReferenceProvider
import com.github.zeldigas.text2confl.convert.markdown.MarkdownFileConverter
import com.github.zeldigas.text2confl.convert.markdown.MarkdownParser
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.writeText

class AsciidocFileConverterTest {

    private val converter = AsciidocFileConverter()

    @Test
    internal fun `Read header with no title and title attribute`(@TempDir dir: Path) {
        val file = dir.resolve("src.adoc")
        file.writeText(
            """
            Some adoc
        """.trimIndent()
        )

        val result = converter.readHeader(file, HeaderReadingContext(titleTransformer()))

        assertThat(result).all {
            prop(PageHeader::title).isEqualTo("Prefixed: src")
            prop(PageHeader::attributes).isNotEmpty()
        }
    }

    @Test
    internal fun `Read header with title frontmatter`(@TempDir dir: Path) {
        val file = dir.resolve("src.adoc")
        file.writeText(
            """
            :title: Custom title
            :labels: a,b,c
            
            = Doc title
            
            and content
        """.trimIndent()
        )

        val result = converter.readHeader(file, HeaderReadingContext(titleTransformer()))

        assertThat(result).all {
            prop(PageHeader::title).isEqualTo("Prefixed: Custom title")
            prop(PageHeader::attributes).contains("labels", "a,b,c")
        }
    }

    @ValueSource(strings = [
        "Simple title", "Title: special chars", "Title & another special chars"
    ])
    @ParameterizedTest
    internal fun `Read header from first header`(title: String, @TempDir dir: Path) {
        val file = dir.resolve("src.md")
        file.writeText(
            """
            :labels: a,b,c
            
            = $title
            
        """.trimIndent()
        )

        val result = converter.readHeader(file, HeaderReadingContext(titleTransformer()))

        assertThat(result).all{
            prop(PageHeader::title).isEqualTo("Prefixed: $title")
            prop(PageHeader::attributes).contains("labels", "a,b,c" )
        }
    }

    @Test
    internal fun `Read header from file header with non first level heading`(@TempDir dir: Path) {
        val file = dir.resolve("src.adoc")
        file.writeText(
            """                               
            == Ignored heading
            
            text
            """.trimIndent()
        )

        val result = converter.readHeader(file, HeaderReadingContext(titleTransformer()))

        assertThat(result).prop(PageHeader::title).isEqualTo("Prefixed: src")
    }

    private fun titleTransformer(): (Path, String) -> String = { _, title -> "Prefixed: $title" }

    @Test
    internal fun `Convert file`(@TempDir dir: Path) {
        val file = dir.resolve("src.adoc")
        file.writeText(
            """
            = Test document
            
            Some document link:test.txt[with attachment]
            
            Code block:
            [source,java]
            ----
            System.out.println("Hello world!");
            ----
            
            === Second level section
            
            https://example.org[Link]
        """.trimIndent()
        )
        Files.createFile(dir.resolve("test.txt"))

        val result = converter.convert(
            file, ConvertingContext(
                ReferenceProvider.singleFile(), ConversionParameters(LanguageMapper.forServer(), titleTransformer(), docRootLocation = "http://example.com/"), "TEST",
            )
        )

        assertThat(result).all {
            prop(PageContent::header).prop(PageHeader::title).isEqualTo("Prefixed: Test document")
            prop(PageContent::attachments).isEqualTo(
                listOf(Attachment("test.txt", "test.txt", dir.resolve("test.txt")))
            )
            prop(PageContent::body).isEqualTo(
                """
            <p><ac:structured-macro ac:name="note"><ac:rich-text-body><p>Edit <a href="http://example.com/src.adoc">source file</a> instead of changing page in Confluence. <span style="color: rgb(122,134,154); font-size: small;">Page was generated from source with <a href="https://github.com/zeldigas/text2confl">text2confl</a>.</span></p></ac:rich-text-body></ac:structured-macro></p>
            <p>Some document <ac:link><ri:attachment ri:filename="test.txt" /><ac:plain-text-link-body><![CDATA[with attachment]]></ac:plain-text-link-body></ac:link></p>
            <p>Code block:</p>
            <ac:structured-macro ac:name="code"><ac:parameter ac:name="language">java</ac:parameter><ac:plain-text-body><![CDATA[System.out.println("Hello world!");]]></ac:plain-text-body></ac:structured-macro>
            <h2>Second level section<ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">second-level-section</ac:parameter></ac:structured-macro></h2>
            <p><a href="https://example.org">Link</a></p>
            """.trimIndent()
            )
        }
    }

    @Test
    @Disabled
    internal fun `Convert file with first header removal`(@TempDir dir: Path) {
        val file = dir.resolve("src.md")
        file.writeText(
            """
            # Page title

            text
        """.trimIndent()
        )

        val result = converter.convert(
            file, ConvertingContext(
                ReferenceProvider.singleFile(), ConversionParameters(LanguageMapper.forServer(), titleTransformer(), addAutogeneratedNote = false), "TEST"
            )
        )

        assertThat(result).all {
            prop(PageContent::header).isEqualTo(PageHeader("Prefixed: Page title", emptyMap()))
            prop(PageContent::body).isEqualTo(
                """
            <p>text</p>
            """.trimIndent() + "\n"
            )
        }
    }

    @Test
    @Disabled
    internal fun `AST parsing failure is thrown as specific exception`(@TempDir dir: Path) {
        val file = dir.resolve("test").createFile().also { it.writeText("test") }

        val cause = IOException("error during file parsing")
        val parser = mockk<MarkdownParser> {
            every { parseReader(any()) } throws cause
        }

        assertThat {
            MarkdownFileConverter(parser).convert(file, mockk() {
                every { titleTransformer } returns mockk()
            })
        }.isFailure().isInstanceOf(ConversionFailedException::class).all {
            hasCause(cause)
            hasMessage("Document parsing failed")
        }


    }

}