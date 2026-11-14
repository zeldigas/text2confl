package com.github.zeldigas.text2confl.convert.markdown.diagram

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.samskivert.mustache.MustacheException
import org.junit.jupiter.api.Test

class InlineDiagramMakerTest {

    private fun maker(template: String, vars: Map<String, Any?> = emptyMap()) =
        InlineDiagramMaker("a&b", InlineDiagramMaker.compile(template), vars)

    private fun InlineDiagramMaker.toContent(script: String, attributes: Map<String, String> = emptyMap()) =
        toDiagram(script, attributes, null).content

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
    fun `Configured variables are expanded`() {
        val result = maker(
            "{{tenantId}}|{{nested.key}}|{{#flags}}<{{.}}>{{/flags}}|{{#enabled}}on{{/enabled}}",
            mapOf("tenantId" to "a<b", "nested" to mapOf("key" to 1), "flags" to listOf("x", "y"), "enabled" to true)
        ).toContent("A")

        assertThat(result).isEqualTo("a&lt;b|1|<x><y>|on")
    }

    @Test
    fun `Code block attributes are available as attributes variable`() {
        val template = "{{attributes.target}}|{{{attributes.title}}}|" +
                "{{#attributes.missing}}set{{/attributes.missing}}{{^attributes.missing}}default{{/attributes.missing}}"

        val result = maker(template).toContent("A", mapOf("target" to "a&b", "title" to "x & y"))

        assertThat(result).isEqualTo("a&amp;b|x & y|default")
    }

    @Test
    fun `Missing attribute fails rendering when used as variable`() {
        assertFailure { maker("{{attributes.target}}").toContent("A") }.isInstanceOf<MustacheException>()
    }

    @Test
    fun `Unknown variable fails rendering`() {
        assertFailure { maker("{{script}}").toContent("A") }.isInstanceOf<MustacheException>()
    }
}
