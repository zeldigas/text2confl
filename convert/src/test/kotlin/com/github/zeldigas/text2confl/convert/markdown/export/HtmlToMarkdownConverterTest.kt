package com.github.zeldigas.text2confl.convert.markdown.export

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class HtmlToMarkdownConverterTest {

    private val converter = HtmlToMarkdownConverter(ConfluenceLinksResolver.NOP, "_assets",
        userResolver = object : ConfluenceUserResolver {
            override fun resolve(userKey: String): String? {
                return when (userKey) {
                    "known" -> "user"
                    "known_email" -> "user@example.org"
                    else -> null
                }
            }
        })

    @ValueSource(
        strings = [
            "basic",
            "code",
            "links",
            "tables",
            "confluence-specific",
            "user-refs",
        ]
    )
    @ParameterizedTest
    fun `Conversion of confluence page`(pageId: String) {
        val input = readResource("/convert/$pageId.html")

        val result = converter.convert(input)

        assertThat(result).isEqualTo(readResource("/convert/$pageId.md"))
    }

    private fun readResource(resource: String): String {
        return HtmlToMarkdownConverter::class.java.getResourceAsStream(resource)?.use {
            String(it.readAllBytes()).replace("\r\n", "\n")
        } ?: throw IllegalStateException("Failed to load $resource")
    }
}