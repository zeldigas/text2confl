package com.github.zeldigas.text2confl.convert.asciidoc

import assertk.assertThat
import org.junit.jupiter.api.Test

internal class RenderingOfConfluenceSpecificFeaturesTest : RenderingTestBase() {

    @Test
    internal fun `Confluence status macro`() {
        val result = toHtml(
            """
            Text with status:red[text of status].
                        
            No text status:green[]
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p>Text with <ac:structured-macro ac:name="status"><ac:parameter ac:name="colour">Red</ac:parameter><ac:parameter ac:name="title">text of status</ac:parameter></ac:structured-macro>.</p>
            <p>No text <ac:structured-macro ac:name="status"><ac:parameter ac:name="colour">Green</ac:parameter></ac:structured-macro></p>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Confluence user macro`() {
        val result = toHtml(
            """
            Hello user:useRname[].    
                    
            Hello user:v.uS_er[].
                        
            Hello user:+++__user__+++[].
            
            Hello user:user-name[].
            
            Hello user:user-name[]-.
            
            Hello user:user@example.com[]-.
                        
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
            <p>Hello <ac:link><ri:user ri:username="user@example.com" /></ac:link>-.</p>
            <p>Hello @..</p>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Confluence user macro is ignored in code literal block`() {
        val result = toHtml(
            """
            Hello `+code block with user:useRname[] user:user-name[] and user:__user__[].+`
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p>Hello <code>code block with user:useRname[] user:user-name[] and user:__user__[].</code></p>
        """.trimIndent()
        )
    }

    @Test
    fun `Simple inline macro generates any structured macro`() {
        val result = toHtml(
            """
            Test confl_macro:jira[key=ABC-123].
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p>Test <ac:structured-macro ac:name="jira"><ac:parameter ac:name="key">ABC-123</ac:parameter></ac:structured-macro>.</p>
        """.trimIndent()
        )
    }
}