package com.github.zeldigas.text2confl.convert.asciidoc

import assertk.assertThat
import org.junit.jupiter.api.Test

internal class RenderingOfTaskListsTest : RenderingTestBase() {

    @Test
    internal fun `Well formatted task list is rendered as confluence task list`() {
        val result = toHtml(
            """
            * [ ] open task
            * [x] closed task
            * [ ] open task _with formatting_
            * [x] complex task with multiline
            +
            block
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ac:task-list>
            <ac:task><ac:task-status>incomplete</ac:task-status><ac:task-body>open task</ac:task-body></ac:task>
            <ac:task><ac:task-status>complete</ac:task-status><ac:task-body>closed task</ac:task-body></ac:task>
            <ac:task><ac:task-status>incomplete</ac:task-status><ac:task-body>open task <em>with formatting</em></ac:task-body></ac:task>
            <ac:task><ac:task-status>complete</ac:task-status><ac:task-body>complex task with multiline<p>block</p></ac:task-body></ac:task></ac:task-list>
        """.trimIndent(),
        )
    }

    @Test
    internal fun `Partial task list is rendered as simple unordered list`() {
        val result = toHtml(
            """
            * [ ] open task
            * [x] closed task
            * [X] closed task 1
            * [ ] open task _with formatting_
            * simple item
            +
            block
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ul>
            <li>[ ] open task</li>
            <li>[X] closed task</li>
            <li>[X] closed task 1</li>
            <li>[ ] open task <em>with formatting</em></li>
            <li>simple item<p>block</p></li></ul>
        """.trimIndent(),
        )
    }
}