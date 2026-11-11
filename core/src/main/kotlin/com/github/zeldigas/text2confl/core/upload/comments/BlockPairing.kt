package com.github.zeldigas.text2confl.core.upload.comments

import com.github.difflib.DiffUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * One-to-one pairing of commented server blocks with generated blocks.
 *
 * 1. Exact: same block group and equal text.
 * 2. Similar: same block group and token similarity above length-dependent threshold.
 *
 * In both passes candidates are assigned greedily: best score first, ties broken by the distance between relative
 * positions of blocks in their documents.
 */
internal class BlockPairing(
    private val serverBlocksCount: Int,
    private val generated: List<TextBlock>,
) {
    fun pair(commented: List<TextBlock>): Map<TextBlock, TextBlock> {
        val pairs = mutableMapOf<TextBlock, TextBlock>()
        val usedGenerated = mutableSetOf<TextBlock>()

        fun assign(candidates: List<Candidate>) {
            candidates
                .sortedWith(compareByDescending<Candidate> { it.score }.thenBy { it.distance })
                .forEach {
                    if (it.server !in pairs && it.generated !in usedGenerated) {
                        pairs[it.server] = it.generated
                        usedGenerated += it.generated
                    }
                }
        }

        assign(commented.flatMap { s ->
            generated.filter { it.group == s.group && it.norm == s.norm }.map { Candidate(s, it, 1.0, distance(s, it)) }
        })

        assign(commented.filter { it !in pairs }.flatMap { s ->
            generated.filter { it.group == s.group && it !in usedGenerated }.mapNotNull { g ->
                val threshold = threshold(max(s.tokens.size, g.tokens.size))
                if (similarityUpperBound(s.tokens.size, g.tokens.size) < threshold) return@mapNotNull null
                val score = similarity(s.tokens, g.tokens)
                if (score >= threshold) Candidate(s, g, score, distance(s, g)) else null
            }
        })

        return pairs
    }

    private fun distance(server: TextBlock, generated: TextBlock): Double =
        abs(relativePosition(server.index, serverBlocksCount) - relativePosition(generated.index, this.generated.size))

    private fun relativePosition(index: Int, count: Int): Double = if (count <= 1) 0.0 else index.toDouble() / (count - 1)

    private class Candidate(val server: TextBlock, val generated: TextBlock, val score: Double, val distance: Double)

    companion object {
        private const val LONG_BLOCK_TOKENS = 10
        private const val SHORT_BLOCK_TOKENS = 2
        private const val LONG_THRESHOLD = 0.8
        private const val SHORT_THRESHOLD = 0.5

        fun threshold(tokens: Int): Double = when {
            tokens >= LONG_BLOCK_TOKENS -> LONG_THRESHOLD
            tokens <= SHORT_BLOCK_TOKENS -> SHORT_THRESHOLD
            else -> SHORT_THRESHOLD + (LONG_THRESHOLD - SHORT_THRESHOLD) *
                    (tokens - SHORT_BLOCK_TOKENS) / (LONG_BLOCK_TOKENS - SHORT_BLOCK_TOKENS)
        }

        private fun similarityUpperBound(a: Int, b: Int): Double = if (a + b == 0) 1.0 else 2.0 * min(a, b) / (a + b)

        /** Dice coefficient over matched tokens: `2·M / (|a| + |b|)` */
        fun similarity(a: List<String>, b: List<String>): Double {
            if (a.isEmpty() && b.isEmpty()) return 1.0
            val changed = DiffUtils.diff(a, b).deltas.sumOf { it.source.size() }
            val matched = a.size - changed
            return 2.0 * matched / (a.size + b.size)
        }
    }
}
