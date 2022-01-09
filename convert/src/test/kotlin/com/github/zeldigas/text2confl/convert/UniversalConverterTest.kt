package com.github.zeldigas.text2confl.convert

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import com.github.zeldigas.text2confl.convert.confluence.LanguageMapper
import com.github.zeldigas.text2confl.convert.confluence.ReferenceProvider
import com.github.zeldigas.text2confl.convert.markdown.MarkdownFileConverter
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createFile

@ExtendWith(MockKExtension::class)
internal class UniversalConverterTest(
    @MockK private val languageMapper: LanguageMapper,
    @MockK private val fileConverter: FileConverter
) {

    val titleConverter: (Path, String) -> String = { _, t -> "Prefixed: $t" }

    val converter = UniversalConverter(
        "TEST", languageMapper, titleConverter, mapOf(
            "t" to fileConverter
        )
    )

    @Test
    internal fun `File conversion`(@TempDir dir: Path) {
        val src = dir.resolve("test.t")
        Files.createFile(src)
        val expectedResult = PageContent(
            PageHeader("test", emptyMap()), "content", emptyList()
        )
        every { fileConverter.convert(src, any()) } returns expectedResult
        val result = converter.convertFile(src)

        assertThat(result).isEqualTo(
            Page(
                expectedResult, src, emptyList()
            )
        )

        verify {
            fileConverter.convert(
                src,
                ConvertingContext(ReferenceProvider.nop(), languageMapper, "TEST", titleConverter)
            )
        }
    }

    @Test
    internal fun `No file conversion for unsupported format`(@TempDir dir: Path) {
        val src = dir.resolve("test.unsupported")

        assertThat { converter.convertFile(src) }.isFailure()
            .isInstanceOf(IllegalArgumentException::class).hasMessage("Unsupported extension: unsupported")
    }

    @Test
    internal fun `File must exist`(@TempDir dir: Path) {
        val src = dir.resolve("anotherTest.t")

        assertThat { converter.convertFile(src) }.isFailure()
            .isInstanceOf(FileDoesNotExistException::class).hasMessage("File does not exist: $src")
    }

    @Test
    internal fun `Directory conversion`(@TempDir dir: Path) {
        createFileStructure(
            dir,
            "one.t",
            "_two.t",
            "three.t",
            "three/foo.t",
            "three/bar.md",
            "four.md",
            "five.adoc",
        )
        every { fileConverter.readHeader(any(), any()) } answers {
            val file = it.invocation.args[0] as Path
            PageHeader(file.toString(), emptyMap())
        }
        every { fileConverter.convert(any(), any()) } answers {
            val file = it.invocation.args[0] as Path
            PageContent(PageHeader(file.toString(), emptyMap()), "Content of $file", emptyList())
        }
        val result = converter.convertDir(dir)

        assertThat(result).hasSize(2)
        assertThat(result.map { it.source }).isEqualTo(listOf(dir.resolve("one.t"), dir.resolve("three.t")))
        assertThat(result.first { it.source == dir.resolve("three.t") }.children).all {
            hasSize(1)
            val child = dir.resolve("three/foo.t")
            transform { it.first() }.isEqualTo(
                Page(
                    PageContent(
                        PageHeader(child.toString(), emptyMap()),
                        "Content of $child",
                        emptyList()
                    ), child, emptyList()
                )
            )
        }
        verify {
            fileConverter.convert(
                dir.resolve("three.t"), ConvertingContext(
                    ReferenceProvider.fromDocuments(dir, mapOf(
                        docAndHeader(dir.resolve("one.t")),
                        docAndHeader(dir.resolve("three.t")),
                        docAndHeader(dir.resolve("three/foo.t")),
                    )),
                    languageMapper, "TEST", titleConverter
                )
            )
        }
    }

    private fun docAndHeader(resolve: Path): Pair<Path, PageHeader> = resolve to PageHeader("$resolve", emptyMap())

    private fun createFileStructure(dir: Path, vararg items: String) {
        items.forEach { item ->
            val itemPath = dir.resolve(item)
            Files.createDirectories(itemPath.parent)
            itemPath.createFile()
        }
    }


    @Test
    internal fun `Factory method for converter`() {

        val result = universalConverter("TEST", languageMapper, titleConverter)

        assertThat(result).isInstanceOf(UniversalConverter::class).all {
            prop(UniversalConverter::titleConverter).isSameAs(titleConverter)
            prop(UniversalConverter::languageMapper).isSameAs(languageMapper)
            prop(UniversalConverter::space).isEqualTo("TEST")
            prop(UniversalConverter::converters).all {
                hasSize(1)
                transform { it["md"] }.isNotNull().isInstanceOf(MarkdownFileConverter::class)
            }
        }
    }
}