package com.github.zeldigas.text2confl.convert.markdown.diagram

import assertk.all
import assertk.assertThat
import assertk.assertions.exists
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.zeldigas.text2confl.convert.Attachment
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.div

private const val CONTENT_HASH = "19fbdde16ffae3f59a48c6b3b40d6796af35bc12da9bc81c2689283a51ae1a97"

@ExtendWith(MockKExtension::class)
class DiagramMakerTest(
    @MockK private val generator: DiagramGenerator
) {

    @Test
    fun `Diagram without explicit name`(@TempDir tempDir: Path) {
        val attributes = mapOf("testAttr" to "value")
        val fullAttributes = attributes + mapOf("lang" to "test")
        val expectedTarget = tempDir / "some" / "prefix" / "testName.abc"
        every {
            generator.name(
                CONTENT_HASH,
                fullAttributes
            )
        } returns "testName.abc"
        val options = mapOf("some" to "attr")
        every { generator.conversionOptions(fullAttributes) } returns options
        every { generator.generate("test script", expectedTarget, options) } returns ImageInfo(1, 1, "hello")

        val maker = DiagramMaker(tempDir, "test", generator)
        val (attachment, info) = maker.toDiagram("test script", attributes, Path.of("some/prefix"))

        thenDiagramGenerated(attachment, expectedTarget, info)
    }

    private fun thenDiagramGenerated(
        attachment: Attachment,
        expectedTarget: Path,
        info: ImageInfo
    ) {
        assertThat(attachment).isEqualTo(Attachment("testName.abc", "_generated_diagram_testName.abc", expectedTarget))
        assertThat(info).isEqualTo(ImageInfo(1, 1, "hello"))
        assertThat(expectedTarget.parent).exists()

        assertThat(expectedTarget.parent / "testName.abc.cache").all {
            exists()
            transform { jacksonObjectMapper().readValue<Map<String, Any>>(it.toFile()) }.isEqualTo(
                mapOf(
                    "imageInfo" to mapOf("height" to 1, "width" to 1, "title" to "hello"),
                    "conversionOptions" to mapOf("some" to "attr"),
                    "checksum" to CONTENT_HASH
                )
            )
        }
    }

    @Test
    fun `Diagram with explicit name`(@TempDir tempDir: Path) {
        val attributes = mapOf("target" to "name", "testAttr" to "value")
        val fullAttributes = attributes + mapOf("lang" to "test")
        val expectedTarget = tempDir / "testName.abc"
        every { generator.name("name", fullAttributes) } returns "testName.abc"
        val options = mapOf("some" to "attr")
        every { generator.conversionOptions(fullAttributes) } returns options
        every { generator.generate("test script", expectedTarget, options) } returns ImageInfo(1, 1, "hello")

        val maker = DiagramMaker(tempDir, "test", generator)
        val (attachment, _) = maker.toDiagram("test script", attributes, null)

        assertThat(attachment).isEqualTo(Attachment("testName.abc", "_generated_diagram_testName.abc", expectedTarget))
    }

    @Test
    fun `Cached diagrams is used when no change`(@TempDir tempDir: Path) {
        val options = mapOf("some" to "attr")
        val (attributes, fullAttributes, expectedTarget) = givenCachePrepared(
            tempDir,
            CONTENT_HASH,
            options
        )
        every {
            generator.name(
                CONTENT_HASH,
                fullAttributes
            )
        } returns "testName.abc"

        every { generator.conversionOptions(fullAttributes) } returns options

        val maker = DiagramMaker(tempDir, "test", generator)
        val (attachment, info) = maker.toDiagram("test script", attributes, Path.of("some/prefix"))

        assertThat(attachment).isEqualTo(Attachment("testName.abc", "_generated_diagram_testName.abc", expectedTarget))
        assertThat(info).isEqualTo(ImageInfo(50, 100, "some title"))
        assertThat(expectedTarget.parent).exists()
    }


    @Test
    fun `Cached diagrams is not used when script changed`(@TempDir tempDir: Path) {
        val options = mapOf("some" to "attr")
        val (attributes, fullAttributes, expectedTarget) = givenCachePrepared(
            tempDir,
            CONTENT_HASH + "1",
            options
        )
        every {
            generator.name(
                CONTENT_HASH,
                fullAttributes
            )
        } returns "testName.abc"
        every { generator.conversionOptions(fullAttributes) } returns options
        every { generator.generate("test script", expectedTarget, options) } returns ImageInfo(1, 1, "hello")

        val maker = DiagramMaker(tempDir, "test", generator)
        val (attachment, info) = maker.toDiagram("test script", attributes, Path.of("some/prefix"))

        thenDiagramGenerated(attachment, expectedTarget, info)
    }

    @Test
    fun `Cached diagrams is not used when attributes changed`(@TempDir tempDir: Path) {
        val options = mapOf("some" to "attr")
        val (attributes, fullAttributes, expectedTarget) = givenCachePrepared(
            tempDir,
            CONTENT_HASH,
            options + mapOf("another" to "attr")
        )
        every {
            generator.name(
                CONTENT_HASH,
                fullAttributes
            )
        } returns "testName.abc"
        every { generator.conversionOptions(fullAttributes) } returns options
        every { generator.generate("test script", expectedTarget, options) } returns ImageInfo(1, 1, "hello")

        val maker = DiagramMaker(tempDir, "test", generator)
        val (attachment, info) = maker.toDiagram("test script", attributes, Path.of("some/prefix"))

        thenDiagramGenerated(attachment, expectedTarget, info)
    }

    private fun givenCachePrepared(
        tempDir: Path, checksum: String, conversionOptions: Map<String, String>
    ): Triple<Map<String, String>, Map<String, String>, Path> {
        val attributes = mapOf("testAttr" to "value")
        val fullAttributes = attributes + mapOf("lang" to "test")
        val expectedTarget = tempDir / "some" / "prefix" / "testName.abc"
        Files.createDirectories(expectedTarget.parent)
        jacksonObjectMapper().writeValue(
            (expectedTarget.parent / "testName.abc.cache").toFile(), mapOf(
                "imageInfo" to mapOf("height" to 50, "width" to 100, "title" to "some title"),
                "conversionOptions" to conversionOptions,
                "checksum" to checksum
            )
        )
        expectedTarget.createFile()
        return Triple(attributes, fullAttributes, expectedTarget)
    }
}