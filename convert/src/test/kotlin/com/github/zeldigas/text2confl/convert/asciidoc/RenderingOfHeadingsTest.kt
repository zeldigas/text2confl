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
            
            [.text-right]
            === Subheader
            
            Par inside
            
            [.text-left]
            === Subheader1
            
            [.text-center]
            === Subheader2
            
            [.text-justify]
            === Subheader3
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <h1>First header<ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">first-header</ac:parameter></ac:structured-macro></h1>
            <p>Some paragraph</p>
            <h2 style="text-align: right;">Subheader<ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">subheader</ac:parameter></ac:structured-macro></h2>
            <p>Par inside</p>
            <h2 style="text-align: left;">Subheader1<ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">subheader1</ac:parameter></ac:structured-macro></h2>
            <h2 style="text-align: center;">Subheader2<ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">subheader2</ac:parameter></ac:structured-macro></h2>
            <h2 style="text-align: justify;">Subheader3<ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">subheader3</ac:parameter></ac:structured-macro></h2>
        """.trimIndent()
        )
    }

}


