package com.github.zeldigas.text2confl.convert.markdown

import assertk.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

internal class RenderingOfTaskListsTest : RenderingTestBase() {

    @ValueSource(strings = ["*", "1."])
    @ParameterizedTest
    internal fun `Well formatted task list is rendered as confluence task list`(marker: String) {
        val result = toHtml(
            """
            $marker [ ] open task
            $marker [x] closed task
            $marker [ ] open task _with formatting_
            $marker [x] complex task with multiline
            
                block
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <ac:task-list>
            <ac:task><ac:task-status>incomplete</ac:task-status><ac:task-body>open task</ac:task-body></ac:task>
            <ac:task><ac:task-status>complete</ac:task-status><ac:task-body>closed task</ac:task-body></ac:task>
            <ac:task><ac:task-status>incomplete</ac:task-status><ac:task-body>open task <em>with formatting</em></ac:task-body></ac:task>
            <ac:task><ac:task-status>complete</ac:task-status><ac:task-body>complex task with multiline
            <p>block</p>
            </ac:task-body></ac:task>
            </ac:task-list>
        """.trimIndent(),
        )
    }

    @CsvSource(value = [
        "*,ul",
        "1.,ol"
    ])
    @ParameterizedTest
    internal fun `Partial task list is rendered as simple unordered list`(marker:String, htmlTag:String) {
        val result = toHtml(
            """
            $marker [ ] open task
            $marker [x] closed task
            $marker [X] closed task 1
            $marker [ ] open task _with formatting_
            $marker simple item
            
                block
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <$htmlTag>
              <li>[ ] open task</li>
              <li>[x] closed task</li>
              <li>[X] closed task 1</li>
              <li>[ ] open task <em>with formatting</em></li>
              <li>
                <p>simple item</p>
                <p>block</p>
              </li>
            </$htmlTag>
        """.trimIndent(),
        )
    }
}