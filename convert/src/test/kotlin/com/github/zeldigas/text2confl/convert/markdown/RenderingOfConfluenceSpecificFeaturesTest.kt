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
                        
            Hello @..            
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p>Hello <ac:link><ri:user ri:username="useRname" /></ac:link>.</p>
            <p>Hello <ac:link><ri:user ri:username="v.uS_er" /></ac:link>.</p>
            <p>Hello <ac:link><ri:user ri:username="__user__" /></ac:link>.</p>
            <p>Hello @..</p>
        """.trimIndent()
        )
    }
}