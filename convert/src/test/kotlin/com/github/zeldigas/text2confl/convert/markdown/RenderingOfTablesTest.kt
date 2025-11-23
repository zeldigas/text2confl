package com.github.zeldigas.text2confl.convert.markdown

import assertk.assertThat
import org.junit.jupiter.api.Test

internal class RenderingOfTablesTest : RenderingTestBase() {

    @Test
    internal fun `Tables rendering`() {
        val result = toHtml(
            """
            | foo | bar |
            |-----|-----|
            | baz | bim |
            {width=75%}
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <table style="width: 75%">
              <thead>
                <tr><th>foo</th><th>bar</th></tr>
              </thead>
              <tbody>
                <tr><td>baz</td><td>bim</td></tr>
              </tbody>
            </table>
        """.trimIndent()
        )
    }

}


