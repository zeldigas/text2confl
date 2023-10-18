package com.github.zeldigas.text2confl.cli

import assertk.all
import assertk.assertFailure
import assertk.assertions.hasMessage
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.context
import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.confclient.ConfluenceClientConfig
import com.github.zeldigas.confclient.PasswordAuth
import com.github.zeldigas.confclient.TokenAuth
import com.github.zeldigas.text2confl.convert.*
import com.github.zeldigas.text2confl.convert.asciidoc.AsciidoctorConfiguration
import com.github.zeldigas.text2confl.convert.markdown.DiagramsConfiguration
import com.github.zeldigas.text2confl.convert.markdown.MarkdownConfiguration
import com.github.zeldigas.text2confl.core.ContentValidationFailedException
import com.github.zeldigas.text2confl.core.ContentValidator
import com.github.zeldigas.text2confl.core.ServiceProvider
import com.github.zeldigas.text2confl.core.config.*
import com.github.zeldigas.text2confl.core.upload.ChangeDetector
import com.github.zeldigas.text2confl.core.upload.ContentUploader
import io.ktor.http.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.div


@ExtendWith(MockKExtension::class)
internal class UploadTest(
    @MockK private val serviceProvider: ServiceProvider,
    @MockK private val contentUploader: ContentUploader,
    @MockK private val confluenceClient: ConfluenceClient,
    @MockK private val contentValidator: ContentValidator,
    @MockK private val converter: Converter
) {
    private val command = Upload()

    @BeforeEach
    internal fun setUp() {
        every { serviceProvider.createConverter(any(), any()) } returns converter
        every { serviceProvider.createConfluenceClient(any(), any()) } returns confluenceClient
        every { serviceProvider.createUploader(confluenceClient, any(), any()) } returns contentUploader
        every { serviceProvider.createContentValidator() } returns contentValidator

        command.context {
            obj = serviceProvider
        }
    }

    @Test
    internal fun `All data from command line args`(@TempDir tempDir: Path) {
        val result = mockk<List<Page>>()
        every { converter.convertDir(tempDir) } returns result
        coEvery { contentUploader.uploadPages(result, "TR", "1234") } just Runs
        every { contentValidator.validate(result) } just Runs

        command.parse(
            listOf(
                "--confluence-url", "https://test.atlassian.net/wiki",
                "--space", "TR",
                "--user", "test",
                "--password", "test",
                "--parent-id", "1234",
                "--remove-orphans", "all",
                "--tenant", "test",
                "--docs", tempDir.toString()
            )
        )

        verify {
            serviceProvider.createConfluenceClient(
                ConfluenceClientConfig(
                    Url("https://test.atlassian.net/wiki"),
                    false,
                    PasswordAuth("test", "test")
                ),
                false
            )
        }
        val expectedConverterConfig = ConverterConfig(
            "", "", EditorVersion.V2, null, null, null,
            CodeBlockParams(), MarkdownConfiguration(diagrams = DiagramsConfiguration(tempDir / ".diagrams")),
            AsciidoctorConfiguration(libsToLoad = listOf("asciidoctor-diagram"), workdir = tempDir / ".asciidoc")
        )
        verify { serviceProvider.createConverter("TR", expectedConverterConfig) }
        verify {
            serviceProvider.createUploader(
                confluenceClient, UploadConfig(
                    "TR", Cleanup.All, "Automated upload by text2confl", true, ChangeDetector.HASH, "test"
                ),
                expectedConverterConfig
            )
        }
    }

    @Test
    internal fun `Data from yaml config file`(@TempDir tempDir: Path) {
        val directoryConfig = sampleConfig().copy(tenant = "test1")
        directoryConfig.docsDir = tempDir
        writeToFile(tempDir.resolve("text2confl.yml"), directoryConfig)

        val result = mockk<List<Page>>()
        every { converter.convertDir(tempDir) } returns result
        coEvery { contentUploader.uploadPages(result, "TR", "1234") } just Runs
        every { contentValidator.validate(result) } just Runs

        command.parse(
            listOf(
                "--access-token", "token",
                "--message", "custom upload message",
                "--docs", tempDir.toString(),
                "--dry"
            )
        )

        verify {
            serviceProvider.createConfluenceClient(
                ConfluenceClientConfig(
                    Url(directoryConfig.server!!),
                    directoryConfig.skipSsl,
                    TokenAuth("token")
                ),
                true
            )
        }
        val converterConfig = ConverterConfig(
            directoryConfig.titlePrefix, directoryConfig.titlePostfix,
            directoryConfig.editorVersion!!, null,
            "http://example.com/", null,
            directoryConfig.codeBlocks,
            directoryConfig.markdown.toConfig(directoryConfig.docsDir),
            directoryConfig.asciidoc.toConfig(directoryConfig.docsDir)
        )
        verify { serviceProvider.createConverter(directoryConfig.space!!, converterConfig) }
        verify {
            serviceProvider.createUploader(
                confluenceClient, UploadConfig(
                    directoryConfig.space!!,
                    directoryConfig.removeOrphans,
                    "custom upload message",
                    directoryConfig.notifyWatchers,
                    directoryConfig.modificationCheck,
                    "test1"
                ),
                converterConfig
            )
        }
    }

    @Test
    internal fun `Any credential type must be specified`(@TempDir tempDir: Path) {
        assertFailure {
            command.parse(
                listOf(
                    "--space", "TR",
                    "--confluence-url", "https://wiki.example.org",
                    "--docs", tempDir.toString()
                )
            )
        }.isInstanceOf(PrintMessage::class).all {
            transform { it.printError }.isTrue()
            hasMessage("Either access token or username/password should be specified")
        }
    }

    @Test
    internal fun `Space is required`(@TempDir tempDir: Path) {
        assertFailure {
            command.parse(
                listOf(
                    "--docs", tempDir.toString()
                )
            )
        }.isInstanceOf(PrintMessage::class).all {
            transform { it.printError }.isTrue()
            hasMessage("Space is not specified. Use `--space` option or `space` in config file")
        }
    }

    @Test
    internal fun `Confluence url is required`(@TempDir tempDir: Path) {
        assertFailure {
            command.parse(
                listOf(
                    "--space", "TR",
                    "--docs", tempDir.toString()
                )
            )
        }.isInstanceOf(PrintMessage::class).all {
            transform { it.printError }.isTrue()
            hasMessage("Confluence url is not specified. Use `--confluence-url` option or `server` in config file")
        }
    }

    @Test
    internal fun `Resolution of page by title`(@TempDir tempDir: Path) {
        val result = mockk<List<Page>>()
        every { converter.convertDir(tempDir) } returns result
        coEvery { confluenceClient.getPage("TR", "Test page").id } returns "1234"
        coEvery { contentUploader.uploadPages(result, "TR", "1234") } just Runs
        every { contentValidator.validate(result) } just Runs
        command.parse(
            listOf(
                "--confluence-url", "https://test.atlassian.net/wiki",
                "--space", "TR",
                "--access-token", "test",
                "--parent", "Test page",
                "--docs", tempDir.toString()
            )
        )

        coVerify {
            contentUploader.uploadPages(result, "TR", "1234")
        }
    }

    @Test
    internal fun `Using home page if not specified`(@TempDir tempDir: Path) {
        val result = mockk<List<Page>>()
        every { converter.convertDir(tempDir) } returns result
        coEvery { confluenceClient.describeSpace("TR", listOf("homepage")).homepage?.id } returns "1234"
        coEvery { contentUploader.uploadPages(result, "TR", "1234") } just Runs
        every { contentValidator.validate(result) } just Runs
        command.parse(
            listOf(
                "--confluence-url", "https://test.atlassian.net/wiki",
                "--space", "TR",
                "--access-token", "test",
                "--docs", tempDir.toString()
            )
        )

        coVerify {
            contentUploader.uploadPages(result, "TR", "1234")
        }

    }

    @Test
    internal fun `Handling of file does not exist exception`(@TempDir tempDir: Path) {
        every { converter.convertDir(tempDir) } throws FileDoesNotExistException(tempDir)

        assertFailure {
            command.parse(
                listOf(
                    "--confluence-url", "https://test.atlassian.net/wiki",
                    "--space", "TR",
                    "--access-token", "test",
                    "--docs", tempDir.toString()
                )
            )
        }.isInstanceOf(PrintMessage::class).hasMessage("File does not exist: $tempDir")
    }

    @Test
    internal fun `Handling of conversion failed exception`(@TempDir tempDir: Path) {
        every { converter.convertDir(tempDir) } throws ConversionFailedException(
            tempDir,
            "Conversion error message",
            RuntimeException("cause")
        )

        assertFailure {
            command.parse(
                listOf(
                    "--confluence-url", "https://test.atlassian.net/wiki",
                    "--space", "TR",
                    "--access-token", "test",
                    "--docs", tempDir.toString()
                )
            )
        }.isInstanceOf(PrintMessage::class)
            .hasMessage("Failed to convert $tempDir: Conversion error message (cause: java.lang.RuntimeException: cause)")
    }

    @Test
    internal fun `Handling of content validation exception`(@TempDir tempDir: Path) {
        val result = mockk<List<Page>>()
        every { converter.convertDir(tempDir) } returns result
        every { contentValidator.validate(result) } throws ContentValidationFailedException(
            listOf(
                "error message1",
                "error message2"
            )
        )

        assertFailure {
            command.parse(
                listOf(
                    "--confluence-url", "https://test.atlassian.net/wiki",
                    "--space", "TR",
                    "--access-token", "test",
                    "--docs", tempDir.toString()
                )
            )
        }.isInstanceOf(PrintMessage::class)
            .hasMessage("Some pages content is invalid:\n1. error message1\n2. error message2")
    }

    private fun sampleConfig(): DirectoryConfig {
        return DirectoryConfig(
            server = "https://test.atlassian.net/wiki",
            skipSsl = true,
            space = "TR",
            defaultParentId = "1234",
            removeOrphans = Cleanup.None,
            notifyWatchers = false,
            titlePrefix = "Prefix: ",
            titlePostfix = " - Postfix",
            editorVersion = EditorVersion.V1,
            modificationCheck = ChangeDetector.CONTENT,
            docsLocation = "http://example.com/"
        )
    }

    private fun writeToFile(dest: Path, config: DirectoryConfig) {
        val mapper = ObjectMapper(YAMLFactory())
            .registerKotlinModule()
            .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
        mapper.writeValue(dest.toFile(), config)
    }
}