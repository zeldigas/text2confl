package com.github.zeldigas.text2confl.convert.markdown.diagram

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.IOException
import kotlin.io.path.Path

@ExtendWith(MockKExtension::class)
class PlantUmlDiagramsGeneratorTest(
    @MockK val commandExecutor: CommandExecutor
) {

    @ValueSource(strings = ["puml", "plantuml"])
    @ParameterizedTest
    fun `Supported formats`(lang: String) {
        assertThat(PlantUmlDiagramsGenerator(commandExecutor = commandExecutor).supports(lang)).isTrue()
    }

    @Test
    fun generatorIsNotAvaialbleWhenDisabled() {
        val generator = PlantUmlDiagramsGenerator(enabled = false, commandExecutor = commandExecutor)

        assertThat(generator.available()).isFalse()
    }

    @Test
    fun `Generator is not available when executable is not available`() {
        val generator = PlantUmlDiagramsGenerator(commandExecutor = commandExecutor)

        every { commandExecutor.commandAvailable("plantuml") } returns false

        assertThat(generator.available()).isFalse()
    }

    @Test
    fun `Generator is not available when command for version fails`() {
        every { commandExecutor.commandAvailable("plantuml") } returns true
        every {
            commandExecutor.execute(
                Command(
                    "plantuml",
                    mutableListOf("-version")
                )
            )
        } throws IOException("plantuml command not found")

        val generator = PlantUmlDiagramsGenerator(commandExecutor = commandExecutor)

        assertThat(generator.available()).isFalse()
    }

    @Test
    fun `Generator available when version command is fine`() {
        every { commandExecutor.commandAvailable("plantuml") } returns true
        every { commandExecutor.execute(Command("plantuml", mutableListOf("-version"))) } returns ExecutionResult(
            0,
            "1.2.3\nsome other info",
            ""
        )

        val generator = PlantUmlDiagramsGenerator(commandExecutor = commandExecutor)

        assertThat(generator.available()).isTrue()
    }

    @Test
    fun `Generation of diagram with default format`() {
        val generator = PlantUmlDiagramsGenerator(command = "custom", commandExecutor = commandExecutor)

        assertThat(generator.name("test")).isEqualTo("test.png")

        every {
            commandExecutor.execute(
                Command(
                    "custom", mutableListOf(
                        "-pipe", "-tpng"
                    ), "test".toByteArray(), Path("test.png")
                )
            )
        } returns ExecutionResult(0, "", "")

        val result = generator.generate("test", Path("test.png"))

        assertThat(result).isEqualTo(ImageInfo())
    }

    @Test
    fun `Generation of diagram with custom format and extra options`() {
        val generator = PlantUmlDiagramsGenerator(
            command = "custom", commandExecutor = commandExecutor
        )

        assertThat(generator.name("test", mapOf("format" to "svg"))).isEqualTo("test.svg")

        every {
            commandExecutor.execute(
                Command(
                    "custom", mutableListOf(
                        "-pipe",
                        "-tsvg",
                        "-theme",
                        "custom",
                    ), "test".toByteArray(), Path("test.svg")
                )
            )
        } returns ExecutionResult(0, "", "")

        val result = generator.generate("test", Path("test.svg"), mapOf("theme" to "custom", "format" to "svg"))

        assertThat(result).isEqualTo(ImageInfo())
    }

    @Test
    fun `Failure during diagram generations produces exception`() {
        val generator = PlantUmlDiagramsGenerator(commandExecutor = commandExecutor)

        assertThat(generator.name("test")).isEqualTo("test.png")

        every {
            commandExecutor.execute(
                Command(
                    "plantuml", mutableListOf(
                        "-pipe", "-tpng"
                    ), "test".toByteArray(), Path("test.png")
                )
            )
        } returns ExecutionResult(1, "", "error text")

        assertFailure {
            generator.generate("test", Path("test.png"))
        }.hasMessage("plantuml execution returned non-zero exit code: 1.\nerror text")
    }
}