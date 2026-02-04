package com.github.zeldigas.text2confl.convert.markdown

import assertk.assertThat
import com.github.zeldigas.text2confl.convert.EditorVersion
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class RenderingOfListsTest : RenderingTestBase() {

    @CsvSource("1.,ol", "*,ul")
    @ParameterizedTest
    internal fun `Simple lists are kept as is`(marker: String, expectedTag: String) {
        val result = toHtml(
            """
            $marker hello
            $marker world
        """.trimIndent()
        , editorVersion = EditorVersion.V2)

        assertThat(result).isEqualToConfluenceFormat(
            """
            <$expectedTag>
              <li>hello</li>
              <li>world</li>
            </$expectedTag>
        """.trimIndent()
        )
    }

    @CsvSource("1.,ol", "*,ul")
    @ParameterizedTest
    fun `Lists with nested data are normalized to explicit paragraph item`(marker: String, expectedTag: String) {
        val result = toHtml(
            """
            $marker hello
                * nested
                * nested world
            $marker world
        """.trimIndent()
            , editorVersion = EditorVersion.V2)

        assertThat(result).isEqualToConfluenceFormat(
            """
            <$expectedTag>
              <li>
                <p>hello</p>
                <ul>
                  <li>nested</li>
                  <li>nested world</li>
                </ul>
              </li>
              <li>world</li>
            </$expectedTag>
        """.trimIndent()
        )

    }

    @Test
    fun `Paragraph list`() {
        val result = toHtml(
            """
            * hello
                * nested
                * nested world
            * world
              
              another paragraph
              
            regular text
        """.trimIndent()
            , editorVersion = EditorVersion.V2)

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ul>
              <li>
                <p>hello</p>
                <ul>
                  <li>nested</li>
                  <li>nested world</li>
                </ul>
              </li>
              <li>
                <p>world</p>
                <p>another paragraph</p>
              </li>
            </ul>
            <p>regular text</p>
        """.trimIndent()
        )

    }
}