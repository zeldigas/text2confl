package com.github.zeldigas.text2confl.convert.asciidoc

import assertk.assertThat
import org.junit.jupiter.api.Test

internal class RenderingOfHeadingsTest : RenderingTestBase() {

    @Test
    internal fun `Headings rendering`() {
        val result = toHtml(
            """
            == First header
            
            Some paragraph
            
            === Subheader
            
            Par inside
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <h1>First header<ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">first-header</ac:parameter></ac:structured-macro></h1>
            <p>Some paragraph</p>
            <h2>Subheader<ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">subheader</ac:parameter></ac:structured-macro></h2>
            <p>Par inside</p>
        """.trimIndent()
        )
    }

}


