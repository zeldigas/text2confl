package com.github.zeldigas.text2confl.cli.config

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.zeldigas.text2confl.convert.confluence.CONFLUENCE_CLOUD_LANGUAGES
import com.github.zeldigas.text2confl.convert.confluence.CONFLUENCE_SERVER_LANGUAGES
import com.github.zeldigas.text2confl.convert.markdown.MarkdownConfiguration
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

internal class ConverterConfigTest {

    @Test
    internal fun `Title mapper from prefix and postfix`() {
        assertThat(
            ConverterConfig(
                "Pre ", " - Post", EditorVersion.V1, null, null, null,
                MarkdownConfiguration()
            ).titleConverter(Path(""), "test")
        )
            .isEqualTo("Pre test - Post")
    }

    @Test
    internal fun `languageMapper for v1 editor`() {
        assertThat(
            ConverterConfig(
                "", "", EditorVersion.V1, null, null, null,
                MarkdownConfiguration()
            ).languageMapper.supportedLanguages
        )
            .isEqualTo(CONFLUENCE_SERVER_LANGUAGES)
    }

    @Test
    internal fun `language mapper for v2 editor`() {
        assertThat(
            ConverterConfig(
                "", "", EditorVersion.V2, null, null, null,
                MarkdownConfiguration()
            ).languageMapper.supportedLanguages
        )
            .isEqualTo(CONFLUENCE_CLOUD_LANGUAGES)

    }
}