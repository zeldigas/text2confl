package com.github.zeldigas.text2confl.convert.markdown

import assertk.assertThat
import org.junit.jupiter.api.Test

internal class RenderingOfRawConfluenceMarkupTest : RenderingTestBase() {

    @Test
    internal fun `Confluence markup with inline custom tags`() {
        val result = toHtml(
            """
            Link to <strong>page and _important_ </strong> <ac-link><ri-page ri:content-title="Manual page"/><ac-plain-text-link-body><![CDATA[unmanaged **page**]]></ac-plain-text-link-body></ac-link>            
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p>Link to <strong>page and <em>important</em> </strong> <ac:link><ri:page ri:content-title="Manual page"/><ac:plain-text-link-body><![CDATA[unmanaged **page**]]></ac:plain-text-link-body></ac:link></p>
        """.trimIndent(),
        )
    }

    @Test
    internal fun `Confluence markup with block custom tags`() {
        val result = toHtml(
            """
            <ac-structured-macro ac:name="expand" ac:schema-version="1">
              <ac-parameter ac:name="title">Title</ac-parameter>
              <ac-rich-text-body>
                    ![](images/test.png)
              </ac-rich-text-body>
            </ac-structured-macro>
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p><ac:structured-macro ac:name="expand" ac:schema-version="1">
            <ac:parameter ac:name="title">Title</ac:parameter>
            <ac:rich-text-body>
            <ac:image><ri:url ri:value="images/test.png" /></ac:image>
            </ac:rich-text-body>
            </ac:structured-macro></p>
        """.trimIndent(),
        )
    }
}