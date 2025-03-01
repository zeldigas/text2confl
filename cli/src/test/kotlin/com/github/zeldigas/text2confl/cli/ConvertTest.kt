package com.github.zeldigas.text2confl.cli

import assertk.assertThat
import assertk.assertions.exists
import assertk.assertions.isFalse
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.obj
import com.github.ajalt.clikt.core.parse
import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.Converter
import com.github.zeldigas.text2confl.convert.Page
import com.github.zeldigas.text2confl.core.ContentValidator
import com.github.zeldigas.text2confl.core.ServiceProvider
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.exists

@ExtendWith(MockKExtension::class)
class ConvertTest(
    @MockK private val serviceProvider: ServiceProvider,
    @MockK private val converter: Converter,
    @MockK private val contentValidator: ContentValidator
) {

    private val command = Convert()

    @BeforeEach
    internal fun setUp() {
        every { serviceProvider.createConverter(any(), any()) } returns converter
        every { serviceProvider.createContentValidator() } returns contentValidator
        every { contentValidator.validate(any()) } just Runs

        command.context {
            obj = serviceProvider
        }
    }

    @Test
    internal fun `Conversion without copy attachments`(@TempDir tempDir: Path) {
        tempDir.resolve("attachment.txt").createFile()
        givenPagesConverted(tempDir)

        val outDir = tempDir / "out"
        command.parse(
            listOf(
                "--docs", tempDir.toString(),
                "--out", outDir.toString()
            )
        )

        assertThat(outDir / "a.html").exists()
        assertThat(outDir / "a" / "b.html").exists()
        assertThat(outDir / "a_attachments").transform { it.exists() }.isFalse()
    }

    @Test
    internal fun `Conversion with simple names`(@TempDir tempDir: Path) {
        tempDir.resolve("attachment.txt").createFile()
        givenPagesConverted(tempDir)

        val outDir = tempDir / "out"
        command.parse(
            listOf(
                "--copy-attachments",
                "--docs", tempDir.toString(),
                "--out", outDir.toString()
            )
        )

        assertThat(outDir / "a.html").exists()
        assertThat(outDir / "a_attachments" / "a_name").exists()
        assertThat(outDir / "a" / "b.html").exists()
    }

    @Test
    internal fun `Conversion with titles names`(@TempDir tempDir: Path) {
        tempDir.resolve("attachment.txt").createFile()
        givenPagesConverted(tempDir)

        val outDir = tempDir / "out"
        command.parse(
            listOf(
                "--copy-attachments",
                "--use-title",
                "--docs", tempDir.toString(),
                "--out", outDir.toString()
            )
        )

        assertThat(outDir / "Special_name_ with _.html").exists()
        assertThat(outDir / "Special_name_ with __attachments" / "a_name").exists()
        assertThat(outDir / "Special_name_ with _" / "Simple - name.html").exists()
    }

    private fun givenPagesConverted(tempDir: Path) {
        val result: List<Page> = listOf(
            mockk<Page> {
                every { source } returns tempDir / "a.md"
                every { content } returns mockk {
                    every { header.title } returns "Special&name: with ^*$#?"
                    every { body } returns "data"
                    every { attachments } returns listOf(Attachment("a_name", "", tempDir / "attachment.txt"))
                }
                every { children } returns listOf(
                    mockk {
                        every { source } returns tempDir / "a" / "b.md"
                        every { content } returns mockk {
                            every { header.title } returns "Simple - name"
                            every { body } returns "another data"
                            every { attachments } returns emptyList()
                        }
                        every { children } returns emptyList()
                    }
                )
            }
        )
        every { converter.convertDir(tempDir) } returns result
    }

}