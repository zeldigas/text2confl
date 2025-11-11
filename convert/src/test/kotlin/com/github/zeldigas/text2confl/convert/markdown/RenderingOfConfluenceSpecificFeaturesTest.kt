package com.github.zeldigas.text2confl.convert.markdown

import assertk.assertThat
import org.junit.jupiter.api.Test

internal class RenderingOfConfluenceSpecificFeaturesTest : RenderingTestBase() {

    @Test
    internal fun `Confluence status macro`() {
        val result = toHtml(
            """
            Text with <status color="red">text of status</status>.            
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p>Text with <ac:structured-macro ac:name="status"><ac:parameter ac:name="title">text of status</ac:parameter><ac:parameter ac:name="colour">Red</ac:parameter></ac:structured-macro>.</p>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Confluence user macro`() {
        val result = toHtml(
            """
            Hello @useRname.    
                    
            Hello @v.uS_er.
                        
            Hello @__user__.
            
            Hello @user-name.
            
            Hello @user-name-.
                        
            Hello @..            
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p>Hello <ac:link><ri:user ri:username="useRname" /></ac:link>.</p>
            <p>Hello <ac:link><ri:user ri:username="v.uS_er" /></ac:link>.</p>
            <p>Hello <ac:link><ri:user ri:username="__user__" /></ac:link>.</p>
            <p>Hello <ac:link><ri:user ri:username="user-name" /></ac:link>.</p>
            <p>Hello <ac:link><ri:user ri:username="user-name" /></ac:link>-.</p>
            <p>Hello @..</p>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Confluence user macro in quotes`() {
        val result = toHtml(
            """
            Hello @"user@example.org".    
                    
            Hello @"v.uS_er@example.org"-.
                        
            Hello @".".            
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p>Hello <ac:link><ri:user ri:username="user@example.org" /></ac:link>.</p>
            <p>Hello <ac:link><ri:user ri:username="v.uS_er@example.org" /></ac:link>-.</p>
            <p>Hello <ac:link><ri:user ri:username="." /></ac:link>.</p>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Confluence user macro is ignored in code block`() {
        val result = toHtml(
            """
            Hello `code block with @useRname @user-name and @__user__.`
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p>Hello <code>code block with @useRname @user-name and @__user__.</code></p>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Details block is rendered as confluence expand macro`() {
        val result = toHtml(
            """
            <details>
            <summary>Some summary test</summary>
            
            Contents of expand block **bold**
            
            </details>
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ac:structured-macro ac:name="expand"><ac:parameter ac:name="title">Some summary test</ac:parameter><ac:rich-text-body>
            <p>Contents of expand block <strong>bold</strong></p>
            </ac:rich-text-body></ac:structured-macro>
        """.trimIndent()
        )
    }
}