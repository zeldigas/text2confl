package com.github.zeldigas.text2confl.core.upload.comments

import com.github.difflib.DiffUtils

internal sealed interface Transfer {
    data class Kept(val start: Int, val end: Int) : Transfer
    data class Lost(val reason: LossReason) : Transfer
}

internal object SpanTransfer {
    private const val MIN_KEPT_SHARE = 0.5
    private const val MIN_BOUNDARY_WORD_SHARE = 2.0 / 3

    /**
     * Maps range `[start, end)` of [old] text to [new] text.
     *
     * - all chars of the range survived: range is kept, text inserted inside the range becomes part of it;
     * - first and last chars of the range survived, at least 2/3 of the chars of the first and last word fragments
     *   survived, and at least half of all chars: range is kept on the mapped text;
     * - otherwise range is lost.
     */
    fun transfer(old: String, new: String, start: Int, end: Int): Transfer {
        if (old == new) return Transfer.Kept(start, end)

        val map = charMapping(old, new)
        val survivors = (start until end).filter { map[it] >= 0 }
        if (survivors.isEmpty()) return Transfer.Lost(LossReason.TEXT_CHANGED)

        val mapped = Transfer.Kept(map[survivors.first()], map[survivors.last()] + 1)
        val total = end - start
        if (survivors.size == total) return mapped

        val firstWordEnd = (start until end).firstOrNull { isSpace(old[it]) } ?: end
        val lastWordStart = ((start until end).lastOrNull { isSpace(old[it]) } ?: (start - 1)) + 1
        val endsSurvived = map[start] >= 0 && map[end - 1] >= 0 &&
                wordSurvived(map, start, firstWordEnd) && wordSurvived(map, lastWordStart, end)
        return if (endsSurvived && survivors.size.toDouble() / total >= MIN_KEPT_SHARE) {
            mapped
        } else {
            Transfer.Lost(LossReason.TEXT_CHANGED)
        }
    }

    /**
     * Boundary word (its part inside the range) survived if most of its chars survived. This keeps a comment when a
     * typo is fixed in the first or last word (`recieves` -> `receives`), but not when the word is replaced.
     */
    private fun wordSurvived(map: IntArray, from: Int, to: Int): Boolean {
        val kept = (from until to).count { map[it] >= 0 }
        return kept.toDouble() / (to - from) >= MIN_BOUNDARY_WORD_SHARE
    }

    /** For every char of [old] - its index in [new], or -1 if char was removed or replaced */
    fun charMapping(old: String, new: String): IntArray {
        val map = IntArray(old.length) { -1 }
        val patch = DiffUtils.diff(old.toList(), new.toList())
        var oldPos = 0
        var newPos = 0
        for (delta in patch.deltas.sortedBy { it.source.position }) {
            while (oldPos < delta.source.position) {
                map[oldPos++] = newPos++
            }
            oldPos = delta.source.position + delta.source.size()
            newPos = delta.target.position + delta.target.size()
        }
        while (oldPos < old.length) {
            map[oldPos++] = newPos++
        }
        return map
    }
}
