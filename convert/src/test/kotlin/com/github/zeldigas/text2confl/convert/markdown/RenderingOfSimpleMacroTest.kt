package com.github.zeldigas.text2confl.convert.markdown

import assertk.assertThat
import org.junit.jupiter.api.Test

internal class RenderingOfSimpleMacroTest : RenderingTestBase() {

    @Test
    internal fun `Unresolved link with NAME and kv options format renders as confluence macro`() {
        val result = toHtml(
            """
            Intro paragraph
            
            As identified in [JIRA key=ABC-123] ticket.
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p>Intro paragraph</p>
            <p>As identified in <ac:structured-macro ac:name="jira"><ac:parameter ac:name="key">ABC-123</ac:parameter></ac:structured-macro> ticket.</p>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Macro parameters sorted by keys`() {
        val result = toHtml(
            """            
            [JIRA jqlQuery="project = ABC" columns=key,summary,assignee,reporter,status maximumIssues=20]
            
            [JIRACHART jql="project = ABC" chartType=pie statType=components]
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p><ac:structured-macro ac:name="jira"><ac:parameter ac:name="columns">key,summary,assignee,reporter,status</ac:parameter><ac:parameter ac:name="jqlQuery">project = ABC</ac:parameter><ac:parameter ac:name="maximumIssues">20</ac:parameter></ac:structured-macro></p>
            <p><ac:structured-macro ac:name="jirachart"><ac:parameter ac:name="chartType">pie</ac:parameter><ac:parameter ac:name="jql">project = ABC</ac:parameter><ac:parameter ac:name="statType">components</ac:parameter></ac:structured-macro></p>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Invalid parameters are not filtered out`() {
        val result = toHtml(
            """            
            [JIRA jqlQuery="project = ABC" columns=key,summary,assignee,reporter,status maximumIssues=20]
            
            [JIRACHART jql="project = ABC" chartType=pie statType=components]
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p><ac:structured-macro ac:name="jira"><ac:parameter ac:name="columns">key,summary,assignee,reporter,status</ac:parameter><ac:parameter ac:name="jqlQuery">project = ABC</ac:parameter><ac:parameter ac:name="maximumIssues">20</ac:parameter></ac:structured-macro></p>
            <p><ac:structured-macro ac:name="jirachart"><ac:parameter ac:name="chartType">pie</ac:parameter><ac:parameter ac:name="jql">project = ABC</ac:parameter><ac:parameter ac:name="statType">components</ac:parameter></ac:structured-macro></p>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Whitelist of macros can be set`() {
        val result = toHtml(
            """            
            [JIRA key="ABC-123"]
            
            [jira key="ABC-124"]
            
            [JIRACHART jql="project = ABC" chartType=pie statType=components]
        """.trimIndent(), config = MarkdownConfiguration(false, supportedMacros = listOf("jira"))
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p><ac:structured-macro ac:name="jira"><ac:parameter ac:name="key">ABC-123</ac:parameter></ac:structured-macro></p>
            <p><ac:structured-macro ac:name="jira"><ac:parameter ac:name="key">ABC-124</ac:parameter></ac:structured-macro></p>
            <p>[JIRACHART jql=&quot;project = ABC&quot; chartType=pie statType=components]</p>
        """.trimIndent()
        )
    }

    @Test
    internal fun `Resolved link in does not render confluence macro`() {
        val result = toHtml(
            """
            Intro paragraph
            
            As identified in [JIRA test=ABC-123] ticket.
            
            [JIRA test=ABC-123]: https://example.org
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p>Intro paragraph</p>
            <p>As identified in <a href="https://example.org">JIRA test=ABC-123</a> ticket.</p>
        """.trimIndent()
        )
    }

}