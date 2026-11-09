package com.github.zeldigas.text2confl.core.upload.comments

import org.jsoup.nodes.Element

/** Range `[start, end)` in [TextBlock.norm] covered by comment [ref] */
internal class CommentSpan(val ref: String, val block: TextBlock, val start: Int, val end: Int) {
    val text: String
        get() = block.norm.substring(start, end)

    override fun toString(): String = "$ref[$start,$end): $text"
}

internal class ServerComments(
    /** All refs in document order */
    val refs: List<String>,
    /** Spans per ref, in document order */
    val spans: Map<String, List<CommentSpan>>,
)

/**
 * Collects comment spans from server [blocks]. Fragments of the same comment that are adjacent in one block (Confluence
 * splits selection on formatting boundaries) are joined into one span.
 */
internal fun collectComments(root: Element, blocks: List<TextBlock>): ServerComments {
    val refs = root.getElementsByTag(MARKER_TAG)
        .mapNotNull { it.refOrNull() }
        .distinct()

    val spans = mutableMapOf<String, MutableList<CommentSpan>>()
    for (block in blocks) {
        block.markers.asSequence()
            .mapNotNull { it.toRange(block) }
            .filter { it.isNotEmpty() }
            .groupBy({ it.ref }, { it.start to it.end })
            .forEach { (ref, refRanges) ->
                joinAdjacent(block, refRanges).forEach { (start, end) ->
                    spans.getOrPut(ref) { mutableListOf() } += CommentSpan(ref, block, start, end)
                }
            }
    }
    return ServerComments(refs, spans)
}

/** Range of a single marker element in [TextBlock.norm]. Can be empty if the marker covers only whitespace. */
private class MarkerRange(val ref: String, val start: Int, val end: Int) {
    fun isNotEmpty() = end > start
}

/** Range of this marker in [block], or `null` for a marker without `ac:ref` */
private fun MarkerOccurrence.toRange(block: TextBlock): MarkerRange? {
    val ref = element.refOrNull() ?: return null
    return MarkerRange(ref, block.normIndexAtOrAfter(rawStart), block.normIndexAtOrAfter(rawEnd))
}

private fun joinAdjacent(block: TextBlock, ranges: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
    val sorted = ranges.sortedBy { it.first }
    val result = mutableListOf<Pair<Int, Int>>()
    var (curStart, curEnd) = sorted.first()
    for ((start, end) in sorted.drop(1)) {
        if (start <= curEnd || block.norm.substring(curEnd, start).isBlank()) {
            curEnd = maxOf(curEnd, end)
        } else {
            result += curStart to curEnd
            curStart = start
            curEnd = end
        }
    }
    result += curStart to curEnd
    return result
}

internal fun Element.refOrNull(): String? = attr(REF_ATTR).takeIf { it.isNotBlank() }
