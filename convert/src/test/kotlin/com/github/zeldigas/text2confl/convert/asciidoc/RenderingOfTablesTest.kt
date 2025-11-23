package com.github.zeldigas.text2confl.convert.asciidoc

import assertk.assertThat
import org.junit.jupiter.api.Test

internal class RenderingOfTablesTest : RenderingTestBase() {

    @Test
    internal fun `Table with no width`() {
        val result = toHtml(
            """            
            |===
            |Column 1, header row |Column 2, header row |Column 3, header row

            |Cell in column 1, row 2
            |Cell in column 2, row 2
            |Cell in column 3, row 2
            
            |===
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """          
            <table><thead><tr><th>Column 1, header row</th><th>Column 2, header row</th><th>Column 3, header row</th></tr></thead><tbody><tr><td>Cell in column 1, row 2</td><td>Cell in column 2, row 2</td><td>Cell in column 3, row 2</td></tr></tbody></table>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Table with width`() {
        val result = toHtml(
            """            
            [width=75%]
            |===
            |Column 1, header row |Column 2, header row |Column 3, header row

            |Cell in column 1, row 2
            |Cell in column 2, row 2
            |Cell in column 3, row 2
            
            |===
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """          
            <table style="width: 75%"><thead><tr><th>Column 1, header row</th><th>Column 2, header row</th><th>Column 3, header row</th></tr></thead><tbody><tr><td>Cell in column 1, row 2</td><td>Cell in column 2, row 2</td><td>Cell in column 3, row 2</td></tr></tbody></table>
        """.trimIndent()
        )
    }

    @Test
    fun `Complex table rendering`() {
        val result = toHtml("""
            [cols="1h,2a",width=75%]
            |===
            | First header as header | Second header
            
            2+| Column spanning 2 columns
            
            | A
            .2+| Cell spanned 2 rows
            
            | B
            
            
            | Header column
            | Column with complex content
            
            [caption=]
            [cols="2,1"]
            !===
            ! Col1 ! Col2
            
            ! C11
            ! C12
            
            !===
            
            |===            
        """.trimIndent())

        assertThat(result).isEqualToConfluenceFormat(
            """
                <table style="width: 75%"><thead><tr><th>First header as header</th><th>Second header</th></tr></thead><tbody><tr><th colspan="2">Column spanning 2 columns</th></tr><tr><th>A</th><td rowspan="2"><div><p>Cell spanned 2 rows</p></div></td></tr><tr><th>B</th></tr><tr><th>Header column</th><td><div><p>Column with complex content</p>
                <table><thead><tr><th>Col1</th><th>Col2</th></tr></thead><tbody><tr><td>C11</td><td>C12</td></tr></tbody></table></div></td></tr></tbody></table>
            """.trimIndent()
        )
    }
}