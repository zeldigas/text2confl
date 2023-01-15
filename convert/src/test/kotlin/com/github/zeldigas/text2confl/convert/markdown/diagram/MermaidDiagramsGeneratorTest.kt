package com.github.zeldigas.text2confl.convert.markdown.diagram

import assertk.assertThat
import assertk.assertions.*
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.IOException
import kotlin.io.path.Path

@ExtendWith(MockKExtension::class)
class MermaidDiagramsGeneratorTest(
    @MockK val commandExecutor: CommandExecutor
) {

    @Test
    fun `Supported formats`() {
        assertThat(MermaidDiagramsGenerator().supports("mermaid")).isTrue()
    }

    @Test
    fun generatorIsNotAvaialbleWhenDisabled() {
        val generator = MermaidDiagramsGenerator(enabled = false)

        assertThat(generator.available()).isFalse()
    }

    @Test
    fun `Generator is not available when executable is not available`() {
        val generator = MermaidDiagramsGenerator(commandExecutor = commandExecutor)

        every { commandExecutor.commandAvailable("mmdc") } returns false

        assertThat(generator.available()).isFalse()
    }

    @Test
    fun `Generator is not available when command for version fails`() {
        every { commandExecutor.commandAvailable("mmdc") } returns true
        every {
            commandExecutor.execute(
                Command(
                    "mmdc",
                    mutableListOf("-V")
                )
            )
        } throws IOException("mmdc command not found")

        val generator = MermaidDiagramsGenerator(commandExecutor = commandExecutor)

        assertThat(generator.available()).isFalse()
    }

    @Test
    fun `Generator available when version command is fine`() {
        every { commandExecutor.commandAvailable("mmdc") } returns true
        every { commandExecutor.execute(Command("mmdc", mutableListOf("-V"))) } returns ExecutionResult(0, "1.2.3", "")

        val generator = MermaidDiagramsGenerator(commandExecutor = commandExecutor)

        assertThat(generator.available()).isTrue()
    }

    @Test
    fun `Generation of diagram with default format`() {
        val generator = MermaidDiagramsGenerator(command = "custom", commandExecutor = commandExecutor)

        assertThat(generator.name("test")).isEqualTo("test.png")

        every {
            commandExecutor.execute(
                Command(
                    "custom", mutableListOf(
                        "--output", "test.png", "--outputFormat", "png", "--quiet"
                    ), "test".toByteArray()
                )
            )
        } returns ExecutionResult(0, "", "")

        val result = generator.generate("test", Path("test.png"))

        assertThat(result).isEqualTo(ImageInfo())
    }

    @Test
    fun `Generation of diagram with custom format and extra options`() {
        val generator = MermaidDiagramsGenerator(
            command = "custom", commandExecutor = commandExecutor,
            configFile = "mermaid-config.json", cssFile = "mermaid.css", puppeterConfig = "/etc/.config.json"
        )

        assertThat(generator.name("test", mapOf("format" to "svg"))).isEqualTo("test.svg")

        every {
            commandExecutor.execute(
                Command(
                    "custom", mutableListOf(
                        "--output",
                        "test.svg",
                        "--outputFormat",
                        "png",
                        "--configFile",
                        "mermaid-config.json",
                        "--cssFile",
                        "mermaid.css",
                        "--puppeteerConfigFile",
                        "/etc/.config.json",
                        "--quiet"
                    ), "test".toByteArray()
                )
            )
        } returns ExecutionResult(0, "", "")

        val result = generator.generate("test", Path("test.svg"))

        assertThat(result).isEqualTo(ImageInfo())
    }

    @Test
    fun `Failure during diagram generations produces exception`() {
        val generator = MermaidDiagramsGenerator(commandExecutor = commandExecutor)

        assertThat(generator.name("test")).isEqualTo("test.png")

        every {
            commandExecutor.execute(
                Command(
                    "mmdc", mutableListOf(
                        "--output", "test.png", "--outputFormat", "png", "--quiet"
                    ), "test".toByteArray()
                )
            )
        } returns ExecutionResult(1, "some error", "")

        assertThat {
            generator.generate("test", Path("test.png"))
        }.isFailure()
            .hasMessage("mmdc execution returned non-zero exit code: 1.\nsome error")
    }
}