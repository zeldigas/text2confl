package com.github.zeldigas.text2confl.convert.asciidoc

import assertk.assertThat
import org.junit.jupiter.api.Test

internal class RenderingOfTocTest : RenderingTestBase() {

    @Test
    internal fun `Table of contents rendering using macro`() {
        val result = toHtml("""
            Intro paragraph
            
            toc::[]
            
            == Hello
            == World
        """.trimIndent())

        assertThat(result).isEqualToConfluenceFormat("""
            <p>Intro paragraph</p>
            <p><ac:structured-macro ac:name="toc" /></p>
            <h1>Hello<ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">hello</ac:parameter></ac:structured-macro></h1>
            <h1>World<ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">world</ac:parameter></ac:structured-macro></h1>
        """.trimIndent())
    }

    @Test
    internal fun `Table of contents rendering using macro with attributes`() {
        val result = toHtml("""
            Intro paragraph
            
            toc::[maxLevel=5,minLevel=2,style=circle,.test,separator=pipe,type=list,include=".*",exclude="smth special"]
            
            == Hello
            == World
        """.trimIndent())

        assertThat(result).isEqualToConfluenceFormat("""
            <p>Intro paragraph</p>
            <p><ac:structured-macro ac:name="toc"><ac:parameter ac:name="maxLevel">5</ac:parameter><ac:parameter ac:name="minLevel">2</ac:parameter><ac:parameter ac:name="style">circle</ac:parameter><ac:parameter ac:name="separator">pipe</ac:parameter><ac:parameter ac:name="type">list</ac:parameter><ac:parameter ac:name="include">.*</ac:parameter><ac:parameter ac:name="exclude">smth special</ac:parameter></ac:structured-macro></p>
            <h1>Hello<ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">hello</ac:parameter></ac:structured-macro></h1>
            <h1>World<ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">world</ac:parameter></ac:structured-macro></h1>
        """.trimIndent())
    }

    @Test
    internal fun `Table of contents using doc attributes`() {
        val result = toHtml("""
            :toc:
            :toclevels: 3
            
            Intro paragraph                       
        """.trimIndent())

        assertThat(result).isEqualToConfluenceFormat("""
            <p><ac:structured-macro ac:name="toc"><ac:parameter ac:name="maxLevel">3</ac:parameter></ac:structured-macro></p>
            <p>Intro paragraph</p>
        """.trimIndent())
    }

    @Test
    internal fun `Table of contents using doc attributes after preable`() {
        val result = toHtml("""            
            = Document
            :toc: preamble  
            
            Intro paragraph                       
            
            == First section
        """.trimIndent())

        assertThat(result).isEqualToConfluenceFormat("""
            <p>Intro paragraph</p>
            <p><ac:structured-macro ac:name="toc" /></p>
            <h1>First section<ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">first-section</ac:parameter></ac:structured-macro></h1>
        """.trimIndent())
    }
}