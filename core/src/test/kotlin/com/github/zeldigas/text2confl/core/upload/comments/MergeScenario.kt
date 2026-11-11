package com.github.zeldigas.text2confl.core.upload.comments

import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.readText

/**
 * Merge scenario loaded from a text file:
 *
 * ```
 * Free-form description
 * === server
 * <p>body from Confluence, with markers</p>
 * === generated
 * <p>body produced by converter</p>
 * === expected
 * <p>expected result (optional, generated body is expected when section is missing)</p>
 * === report
 * c1 KEPT
 * c2 ADJUSTED "new commented text"
 * c3 LOST TEXT_CHANGED
 * c4 PARTIAL BLOCK_NOT_FOUND
 * ```
 *
 * Report has a line per comment ref: status, optional loss reason and optional expected new text in quotes.
 *
 * In all sections markers can be written in short form: `{c1:commented text:}` is the same as
 * `<ac:inline-comment-marker ac:ref="c1">commented text</ac:inline-comment-marker>`.
 */
class MergeScenario(
    val name: String,
    val description: String,
    val server: String,
    val generated: String,
    val expected: String,
    val report: List<ExpectedOutcome>,
) {
    override fun toString(): String = name

    companion object {
        private val SECTION = Regex("^=== (\\S+)\\s*$")
        private val REPORT_LINE = Regex("""^(\S+)\s+(KEPT|ADJUSTED|PARTIAL|LOST)(?:\s+([A-Z_]+))?(?:\s+"(.*)")?\s*$""")

        fun load(file: Path): MergeScenario {
            val sections = mutableMapOf<String, StringBuilder>()
            var current = sections.getOrPut("description") { StringBuilder() }
            for (line in file.readText().lines()) {
                val header = SECTION.find(line)
                if (header != null) {
                    current = sections.getOrPut(header.groupValues[1]) { StringBuilder() }
                } else {
                    current.append(line).append('\n')
                }
            }
            fun section(name: String) = sections[name]?.toString()?.trim('\n')?.let(::expandMarkers)
            val generated = section("generated") ?: error("No generated section in $file")
            return MergeScenario(
                name = file.name.removeSuffix(".txt"),
                description = section("description").orEmpty().trim(),
                server = section("server") ?: error("No server section in $file"),
                generated = generated,
                expected = section("expected") ?: generated,
                report = section("report").orEmpty().lines().filter { it.isNotBlank() }.map { line ->
                    val match = REPORT_LINE.find(line.trim()) ?: error("Bad report line in $file: $line")
                    ExpectedOutcome(
                        ref = match.groupValues[1],
                        status = CommentStatus.valueOf(match.groupValues[2]),
                        reason = match.groupValues[3].takeIf { it.isNotEmpty() }?.let { LossReason.valueOf(it) },
                        newText = match.groups[4]?.value,
                    )
                },
            )
        }
    }
}

internal fun expandMarkers(text: String) = text
    .replace(Regex("""\{([\w-]+):""")) { "<$MARKER_TAG $REF_ATTR=\"${it.groupValues[1]}\">" }
    .replace(":}", "</$MARKER_TAG>")

data class ExpectedOutcome(val ref: String, val status: CommentStatus, val reason: LossReason?, val newText: String?)
