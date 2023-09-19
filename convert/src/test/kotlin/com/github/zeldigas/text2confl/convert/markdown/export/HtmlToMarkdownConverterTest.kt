package com.github.zeldigas.text2confl.convert.markdown.export

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class HtmlToMarkdownConverterTest {

    private val converter = HtmlToMarkdownConverter(ConfluenceLinksResolver.NOP, "_assets")

    @ValueSource(
        strings = [
            "basic",
            "code",
            "links",
            "tables",
            "confluence-specific",
        ]
    )
    @ParameterizedTest
    fun `Conversion of confluence page`(pageId: String) {
        val input = readResoource("/convert/$pageId.html")

        val result = converter.convert(input)

        assertThat(result).isEqualTo(readResoource("/convert/$pageId.md"))
    }

    private fun readResoource(resource: String): String {
        return HtmlToMarkdownConverter::class.java.getResourceAsStream(resource)?.use {
            String(it.readAllBytes())
        } ?: throw IllegalStateException("Failed to load $resource")
    }
}