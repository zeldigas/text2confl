package com.github.zeldigas.text2confl.core.upload.comments

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.toPath

/**
 * Runs every scenario from `inline-comments/scenarios`. See [MergeScenario] for the file format.
 *
 * Besides the expected body and report, every scenario checks invariants of the merge:
 * - text of every block in the result is the same as in generated body (markers never change text);
 * - merge is stable: merging the result with the same generated body gives the same result with all comments kept.
 */
internal class InlineCommentsMergerScenariosTest {

    private val merger = InlineCommentsMerger()

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    fun `merge scenario`(scenario: MergeScenario) {
        val result = merger.merge(scenario.server, scenario.generated)

        assertThat(result.error).isNull()
        assertThat(CanonicalXml.canonical(result.body), "body").isEqualTo(CanonicalXml.canonical(scenario.expected))
        assertThat(result.comments.map { it.toExpected(scenario.report.find { e -> e.ref == it.ref }) }, "report")
            .isEqualTo(scenario.report)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    fun `markers do not change block text`(scenario: MergeScenario) {
        val result = merger.merge(scenario.server, scenario.generated)

        assertThat(blockTexts(result.body)).isEqualTo(blockTexts(scenario.generated))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    fun `merge is stable`(scenario: MergeScenario) {
        val first = merger.merge(scenario.server, scenario.generated)
        val second = merger.merge(first.body, scenario.generated)

        assertThat(CanonicalXml.canonical(second.body)).isEqualTo(CanonicalXml.canonical(first.body))
        assertThat(second.comments.map { it.status }.distinct().filter { it != CommentStatus.KEPT }).isEqualTo(emptyList())
    }

    private fun blockTexts(body: String) = collectBlocks(StorageXml.parse(body)).map { "${it.kind}: ${it.norm}" }

    private fun CommentOutcome.toExpected(expected: ExpectedOutcome?) = ExpectedOutcome(
        ref, status, reason, newText.takeIf { expected?.newText != null }
    )

    companion object {
        @JvmStatic
        fun scenarios(): List<MergeScenario> {
            val dir: Path = InlineCommentsMergerScenariosTest::class.java.getResource("/inline-comments/scenarios")!!
                .toURI().toPath()
            return dir.listDirectoryEntries()
                .filter { it.extension == "txt" }
                .sorted()
                .map { MergeScenario.load(it) }
        }
    }
}
