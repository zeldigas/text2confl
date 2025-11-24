package com.github.zeldigas.text2confl.convert.markdown

import assertk.assertThat
import org.junit.jupiter.api.Test

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

}


