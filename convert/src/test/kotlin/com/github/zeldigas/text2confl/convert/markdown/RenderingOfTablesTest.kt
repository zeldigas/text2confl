package com.github.zeldigas.text2confl.convert.markdown

import assertk.assertThat
import com.github.zeldigas.text2confl.convert.EditorVersion
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class RenderingOfTablesTest : RenderingTestBase() {

    @Test
    internal fun `Tables rendering`() {
        val result = toHtml(
            """
            | foo | bar | baz | 
            |-----|:-----:|-----:|
            | baz | bim | abc |
            {width=75%}
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <table style="width: 75%">
              <thead>
                <tr><th>foo</th><th style="text-align: center;">bar</th><th style="text-align: right;">baz</th></tr>
              </thead>
              <tbody>
                <tr><td>baz</td><td style="text-align: center;">bim</td><td style="text-align: right;">abc</td></tr>
              </tbody>
            </table>
        """.trimIndent()
        )
    }

    @CsvSource(
        "full-width,width=75%,1350,align-start",
        "full-width,width=75% align=center,1350,center",
        "default,width=75%,1350,center",
        "full-width,abc=1,1800,align-start",
        "default,abc=1,760,default",
    )
    @ParameterizedTest
    internal fun `Tables rendering in v2 editor`(prop: String, attrs: String, width: String, layout: String) {
        val result = toHtml(
            """
            | foo | bar | baz | 
            |-----|:-----:|-----:|
            | baz | bim | abc |
            {$attrs}
        """.trimIndent(),
            editorVersion = EditorVersion.V2, pageAttributes = mapOf("property_content-appearance-published" to prop)
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <table data-table-width="$width" data-layout="$layout">
              <thead>
                <tr><th>foo</th><th style="text-align: center;">bar</th><th style="text-align: right;">baz</th></tr>
              </thead>
              <tbody>
                <tr><td>baz</td><td style="text-align: center;">bim</td><td style="text-align: right;">abc</td></tr>
              </tbody>
            </table>
        """.trimIndent()
        )
    }

}


