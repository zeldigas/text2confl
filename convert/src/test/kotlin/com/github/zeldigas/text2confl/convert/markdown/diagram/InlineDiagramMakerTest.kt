package com.github.zeldigas.text2confl.convert.markdown.diagram

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.samskivert.mustache.MustacheException
import org.junit.jupiter.api.Test

class InlineDiagramMakerTest {

    private fun maker(template: String) = InlineDiagramMaker("a&b", InlineDiagramMaker.compile(template))

    @Test
    fun `Template variables are expanded`() {
        val result = maker("{{lang}}|{{source}}|{{{source}}}|{{#cdata}}{{{source}}}{{/cdata}}")
            .toContent("""x ]]> y < & "q" 'q' {{lang}}""")

        assertThat(result).isEqualTo(
            "a&amp;b" +
                    "|x ]]&gt; y &lt; &amp; &quot;q&quot; &#39;q&#39; {{lang}}" +
                    """|x ]]> y < & "q" 'q' {{lang}}""" +
                    """|<![CDATA[x ]]]]><![CDATA[> y < & "q" 'q' {{lang}}]]>"""
        )
    }

    @Test
    fun `Whitespace and line breaks of template and source are kept`() {
        val result = maker("<macro>\n  {{#cdata}}{{{source}}}{{/cdata}}\n</macro>").toContent("A\n\n  B")

        assertThat(result).isEqualTo("<macro>\n  <![CDATA[A\n\n  B]]>\n</macro>")
    }

    @Test
    fun `Unknown variable fails rendering`() {
        assertFailure { maker("{{script}}").toContent("A") }.isInstanceOf<MustacheException>()
    }
}
