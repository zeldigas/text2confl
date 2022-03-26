package com.github.zeldigas.text2confl.convert.markdown

import assertk.assertThat
import org.junit.jupiter.api.Test

internal class RenderingOfJiraReferenceTest : RenderingTestBase() {

    @Test
    internal fun `Unresolved link in JIRA ref format renders jira single issue macro`() {
        val result = toHtml("""
            Intro paragraph
            
            As identified in [JIRA:ABC-123] ticket.
        """.trimIndent())

        assertThat(result).isEqualToConfluenceFormat("""
            <p>Intro paragraph</p>
            <p>As identified in <ac:structured-macro ac:name="jira"><ac:parameter ac:name="key">ABC-123</ac:parameter></ac:structured-macro> ticket.</p>
        """.trimIndent())
    }

    @Test
    internal fun `Resolved link in JIRA ref format does not render jira issue macro`() {
        val result = toHtml("""
            Intro paragraph
            
            As identified in [JIRA:ABC-123] ticket.
            
            [JIRA:ABC-123]: https://example.org
        """.trimIndent())

        assertThat(result).isEqualToConfluenceFormat("""
            <p>Intro paragraph</p>
            <p>As identified in <a href="https://example.org">JIRA:ABC-123</a> ticket.</p>
        """.trimIndent())
    }

}