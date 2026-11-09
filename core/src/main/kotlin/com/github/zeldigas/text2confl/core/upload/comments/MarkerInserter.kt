package com.github.zeldigas.text2confl.core.upload.comments

import org.jsoup.nodes.CDataNode
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Inserts comment markers into generated content the way Confluence does:
 *
 * - contiguous siblings fully covered by the range are wrapped by one marker;
 * - text nodes are split at range boundaries;
 * - formatting elements (`a`, `strong`, `code`, `span`, ...) are never wrapped, marker always goes inside them, even
 *   when the element is fully covered (`<a><marker>whole link</marker></a>`). Several markers with the same ref are
 *   created in this case;
 * - partially covered `ac:link` is split into several links to the same target, markers are never placed inside link.
 *
 * Wrapping never changes the flattened text, so raw offsets stay valid after insertion.
 */
internal object MarkerInserter {

    /** Wraps raw range `[start, end)` of [parent] content into marker with [ref] */
    fun wrap(parent: Element, start: Int, end: Int, ref: String) {
        if (start >= end) return
        splitBoundaries(parent, start, end)
        wrapCovered(parent, start, end, ref)
    }

    private fun splitBoundaries(parent: Element, start: Int, end: Int) {
        var pos = 0
        for (child in parent.childNodes().toList()) {
            val length = rawLength(child)
            val childStart = pos
            val childEnd = pos + length
            pos = childEnd
            if (length == 0 || childEnd <= start || childStart >= end || (start <= childStart && childEnd <= end)) continue

            if (child is TextNode && child !is CDataNode) {
                if (end < childEnd) child.splitText(end - childStart)
                if (start > childStart) child.splitText(start - childStart)
            } else if (child is Element && roleOf(child) == Role.LINK) {
                splitLink(child, listOf(start - childStart, end - childStart).filter { it in 1 until length })
            }
        }
    }

    private fun wrapCovered(parent: Element, start: Int, end: Int, ref: String) {
        val group = mutableListOf<Node>()
        val pending = mutableListOf<Node>()
        fun flush() {
            if (group.isNotEmpty()) wrapNodes(group, ref)
            group.clear()
            pending.clear()
        }

        var pos = 0
        for (child in parent.childNodes().toList()) {
            val length = rawLength(child)
            val childStart = pos
            val childEnd = pos + length
            pos = childEnd
            when {
                length == 0 -> {
                    if (child is Element && roleOf(child) == Role.SKIP) {
                        flush()
                    } else if (group.isNotEmpty() && childStart < end) {
                        pending += child
                    }
                }

                childEnd <= start || childStart >= end -> flush()
                start <= childStart && childEnd <= end && !isFormatting(child) -> {
                    group += pending
                    pending.clear()
                    group += child
                }

                else -> {
                    flush()
                    if (child is Element && roleOf(child) == Role.TRANSPARENT) {
                        wrap(child, maxOf(start, childStart) - childStart, minOf(end, childEnd) - childStart, ref)
                    }
                }
            }
        }
        flush()
    }

    /** Formatting element: marker goes inside it. Existing markers are wrapped as a whole (nested comments). */
    private fun isFormatting(node: Node) =
        node is Element && node.normalName() != MARKER_TAG && roleOf(node) == Role.TRANSPARENT

    private fun wrapNodes(nodes: List<Node>, ref: String) {
        val marker = Element(MARKER_TAG).attr(REF_ATTR, ref)
        nodes.first().before(marker)
        nodes.forEach { marker.appendChild(it) }
    }

    /** Replaces [link] with several copies, each having part of the original body text between consecutive [cuts] */
    private fun splitLink(link: Element, cuts: List<Int>) {
        if (cuts.isEmpty()) return
        val bounds = listOf(0) + cuts.sorted().distinct() + rawLength(link)
        for ((from, to) in bounds.zipWithNext()) {
            val piece = link.clone()
            truncateLink(piece, from, to)
            link.before(piece)
        }
        link.remove()
    }

    private fun truncateLink(link: Element, from: Int, to: Int) {
        val leaves = mutableListOf<Triple<Node, Int, Int>>()
        var pos = 0
        linkBodies(link).forEach { body ->
            walkChildren(body, object : TextVisitor {
                override fun text(node: TextNode) {
                    leaves += Triple(node, pos, pos + node.wholeText.length)
                    pos += node.wholeText.length
                }

                override fun atom(node: Element, char: Char) {
                    leaves += Triple(node, pos, pos + 1)
                    pos += 1
                }
            })
        }
        for ((node, leafStart, leafEnd) in leaves) {
            val keepFrom = maxOf(from, leafStart)
            val keepTo = minOf(to, leafEnd)
            when {
                keepFrom >= keepTo -> node.remove()
                node is TextNode -> node.text(node.wholeText.substring(keepFrom - leafStart, keepTo - leafStart))
            }
        }
    }
}
