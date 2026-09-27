package com.github.zeldigas.text2confl.convert.asciidoc

import assertk.assertThat
import org.junit.jupiter.api.Test

internal class RenderingOfListsTest : RenderingTestBase() {

    @Test
    fun `Ordered list test`() {
        val result = toHtml(
            """
            . plain item
            . wrapped
              content
            . complex item
            +
            paragraph one
            continued
            +
            paragraph two
            continued
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ol><li>plain item</li><li>wrapped content</li><li>complex item<p>paragraph one continued</p>
            <p>paragraph two continued</p></li></ol>
        """.trimIndent()
        )
    }

    @Test
    fun `Unordered list test`() {
        val result = toHtml(
            """
            * plain item
            * wrapped
              content
            * complex item
            +
            paragraph one
            continued
            +
            paragraph two
            continued
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ul>
            <li>plain item</li>
            <li>wrapped content</li>
            <li>complex item<p>paragraph one continued</p>
            <p>paragraph two continued</p></li></ul>
        """.trimIndent()
        )
    }

}
