package com.github.zeldigas.text2confl.convert.markdown

import assertk.assertThat
import org.junit.jupiter.api.Test

internal class RenderingOfTocTest : RenderingTestBase() {

    @Test
    internal fun `Table of contents rendering`() {
        val result = toHtml(
            """
            Intro paragraph
            
            [TOC]
            
            ## Hello
            ## World
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p>Intro paragraph</p>
            <p><ac:structured-macro ac:name="toc" /></p>
            <h2>Hello<ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">hello</ac:parameter></ac:structured-macro></h2>
            <h2>World<ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">world</ac:parameter></ac:structured-macro></h2>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Table of contents rendering with parameters`() {
        val result = toHtml(
            """
            Intro paragraph
            
            [TOC]
            { maxLevel=5 minLevel=2 style=circle .test separator=pipe type=list include=".*" exclude="smth special" }
            
            ## Hello
            ## World
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p>Intro paragraph</p>
            <p>
            <ac:structured-macro ac:name="toc">
              <ac:parameter ac:name="maxLevel">5</ac:parameter>
              <ac:parameter ac:name="minLevel">2</ac:parameter>
              <ac:parameter ac:name="style">circle</ac:parameter>
              <ac:parameter ac:name="separator">pipe</ac:parameter>
              <ac:parameter ac:name="type">list</ac:parameter>
              <ac:parameter ac:name="include">.*</ac:parameter>
              <ac:parameter ac:name="exclude">smth special</ac:parameter>
            </ac:structured-macro>
            </p>
            <h2>Hello<ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">hello</ac:parameter></ac:structured-macro></h2>
            <h2>World<ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">world</ac:parameter></ac:structured-macro></h2>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Table of contents with attributes inside toc`() {
        val result = toHtml(
            """
            Intro paragraph
            
            [TOC maxLevel=5 minLevel=2 style=circle .test separator=pipe type=list include=".*" exclude="smth special"]
            
            ## Hello
            ## World
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p>Intro paragraph</p>
            <p>
            <ac:structured-macro ac:name="toc">
              <ac:parameter ac:name="maxLevel">5</ac:parameter>
              <ac:parameter ac:name="minLevel">2</ac:parameter>
              <ac:parameter ac:name="style">circle</ac:parameter>
              <ac:parameter ac:name="separator">pipe</ac:parameter>
              <ac:parameter ac:name="type">list</ac:parameter>
              <ac:parameter ac:name="include">.*</ac:parameter>
              <ac:parameter ac:name="exclude">smth special</ac:parameter>
            </ac:structured-macro>
            </p>
            <h2>Hello<ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">hello</ac:parameter></ac:structured-macro></h2>
            <h2>World<ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">world</ac:parameter></ac:structured-macro></h2>
        """.trimIndent()
        )
    }
}