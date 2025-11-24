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
            <table class="relative-table"><colgroup><col style="width: 33.3333%;" /><col style="width: 33.3333%;" /><col style="width: 33.3334%;" /></colgroup><thead><tr><th>Column 1, header row</th><th>Column 2, header row</th><th>Column 3, header row</th></tr></thead><tbody><tr><td>Cell in column 1, row 2</td><td>Cell in column 2, row 2</td><td>Cell in column 3, row 2</td></tr></tbody></table>
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
            <table class="relative-table" style="width: 75%"><colgroup><col style="width: 33.3333%;" /><col style="width: 33.3333%;" /><col style="width: 33.3334%;" /></colgroup><thead><tr><th>Column 1, header row</th><th>Column 2, header row</th><th>Column 3, header row</th></tr></thead><tbody><tr><td>Cell in column 1, row 2</td><td>Cell in column 2, row 2</td><td>Cell in column 3, row 2</td></tr></tbody></table>
        """.trimIndent()
        )
    }

    @Test
    fun `Tables text alignment and color in v1 editor`() {
        val result = toHtml(
            """            
            [cols=".^,>.>,^.^"]
            |===
            |foo |baz |bar
                        
            |a
            {set:cellbgcolor:#bf2600}            
            |b            
            {set:cellbgcolor!}
            |c
            
            |===
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """          
            <table class="relative-table"><colgroup><col style="width: 33.3333%;" /><col style="width: 33.3333%;" /><col style="width: 33.3334%;" /></colgroup><thead><tr><th style="vertical-align: middle;">foo</th><th style="text-align: right; vertical-align: bottom;">baz</th><th style="text-align: center; vertical-align: middle;">bar</th></tr></thead><tbody><tr><td style="vertical-align: middle;" data-highlight-colour="#bf2600" class="highligh-#bf2600">a</td><td style="text-align: right; vertical-align: bottom;">b</td><td style="text-align: center; vertical-align: middle;">c</td></tr></tbody></table>
        """.trimIndent()
        )
    }

    @Test
    fun `Tables text alignment and color in v2 editor`() {
        val result = toHtml(
            """            
            [cols=".^,>.>,^.^"]
            |===
            |foo |baz |bar
                        
            |a
            {set:cellbgcolor:#bf2600}            
            |b            
            {set:cellbgcolor!}
            |c
            
            |===
        """.trimIndent(), attributes = mapOf("t2c-editor-version" to "v2")
        )

        assertThat(result).isEqualToConfluenceFormat(
            """          
            <table class="relative-table"><colgroup><col style="width: 33.3333%;" /><col style="width: 33.3333%;" /><col style="width: 33.3334%;" /></colgroup><thead><tr><th>foo</th><th style="text-align: right;">baz</th><th style="text-align: center;">bar</th></tr></thead><tbody><tr><td data-highlight-colour="#bf2600">a</td><td style="text-align: right;">b</td><td style="text-align: center;">c</td></tr></tbody></table>
        """.trimIndent()
        )
    }

    @Test
    fun `Table style rendering`() {
        val result = toHtml(
            """
            [cols="1,2a,2l,2m,2s,2e"]
            |===
            
            | regular | asciidoc styled | literal | monospaced | strong | emphasized
            
            |===
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """<table class="relative-table"><colgroup><col style="width: 9.0909%;" /><col style="width: 18.1818%;" /><col style="width: 18.1818%;" /><col style="width: 18.1818%;" /><col style="width: 18.1818%;" /><col style="width: 18.1819%;" /></colgroup>"""
            + """<tbody><tr><td>regular</td><td><div><p>asciidoc styled</p></div></td><td><div class="literal"><pre> literal</pre></div></td>"""
            + """<td><code>monospaced</code></td><td><strong>strong</strong></td><td><em>emphasized</em></td></tr></tbody></table>"""

        )
    }

    @Test
    fun `Complex table rendering`() {
        val result = toHtml(
            """
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
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
                <table class="relative-table" style="width: 75%"><colgroup><col style="width: 33.3333%;" /><col style="width: 66.6667%;" /></colgroup><thead><tr><th>First header as header</th><th>Second header</th></tr></thead><tbody><tr><th colspan="2">Column spanning 2 columns</th></tr><tr><th>A</th><td rowspan="2"><div><p>Cell spanned 2 rows</p></div></td></tr><tr><th>B</th></tr><tr><th>Header column</th><td><div><p>Column with complex content</p>
                <table class="relative-table"><colgroup><col style="width: 66.6666%;" /><col style="width: 33.3334%;" /></colgroup><thead><tr><th>Col1</th><th>Col2</th></tr></thead><tbody><tr><td>C11</td><td>C12</td></tr></tbody></table></div></td></tr></tbody></table>
            """.trimIndent()
        )
    }
}