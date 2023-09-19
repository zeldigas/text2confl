package com.github.zeldigas.text2confl.convert.markdown.diagram

import assertk.assertThat
import assertk.assertions.exists
import assertk.assertions.isEqualTo
import com.github.zeldigas.text2confl.convert.Attachment
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.div

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
                "19fbdde16ffae3f59a48c6b3b40d6796af35bc12da9bc81c2689283a51ae1a97",
                fullAttributes
            )
        } returns "testName.abc"
        every { generator.generate("test script", expectedTarget, fullAttributes) } returns ImageInfo(1, 1, "hello")

        val maker = DiagramMaker(tempDir, "test", generator)
        val (attachment, info) = maker.toDiagram("test script", attributes, Path.of("some/prefix"))

        assertThat(attachment).isEqualTo(Attachment("testName.abc", "_generated_diagram_testName.abc", expectedTarget))
        assertThat(info).isEqualTo(ImageInfo(1, 1, "hello"))
        assertThat(expectedTarget.parent).exists()
    }

    @Test
    fun `Diagram with explicit name`(@TempDir tempDir: Path) {
        val attributes = mapOf("target" to "name", "testAttr" to "value")
        val fullAttributes = attributes + mapOf("lang" to "test")
        val expectedTarget = tempDir / "testName.abc"
        every { generator.name("name", fullAttributes) } returns "testName.abc"
        every { generator.generate("test script", expectedTarget, fullAttributes) } returns ImageInfo(1, 1, "hello")

        val maker = DiagramMaker(tempDir, "test", generator)
        val (attachment, _) = maker.toDiagram("test script", attributes, null)

        assertThat(attachment).isEqualTo(Attachment("testName.abc", "_generated_diagram_testName.abc", expectedTarget))
    }
}