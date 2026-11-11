package com.github.zeldigas.text2confl.core.upload.comments

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

internal const val MARKER_TAG = "ac:inline-comment-marker"
internal const val REF_ATTR = "ac:ref"
internal const val OBJECT_CHAR = '￼'

private val BLOCK_TAGS = setOf(
    "p", "h1", "h2", "h3", "h4", "h5", "h6", "li", "td", "th", "dt", "dd", "blockquote", "caption", "figcaption"
)
private val CONTAINER_TAGS = setOf("li", "td", "th", "dd", "blockquote")

/** Elements that never contribute text to a block they are nested in. Blocks inside them are collected separately. */
private val STRUCTURE_TAGS = setOf(
    "ul", "ol", "dl", "table", "thead", "tbody", "tfoot", "tr", "colgroup", "col", "div", "pre", "hr",
    "ac:task-list", "ac:layout", "ac:layout-section", "ac:layout-cell",
    "ac:rich-text-body", "ac:plain-text-body", "ac:parameter", "ac:placeholder",
    "ac:adf-content", "ac:adf-attribute", ADF_FALLBACK,
)
private val MACRO_TAGS = setOf("ac:structured-macro", "ac:macro")

/**
 * Cloud ADF extensions (e.g. note panel) repeat their content in `ac:adf-fallback` for renderers that don't support
 * ADF. Editor shows `ac:adf-content`, so fallback is never a source or target of comments.
 */
private const val ADF_EXTENSION = "ac:adf-extension"
private const val ADF_FALLBACK = "ac:adf-fallback"
private val LINK_BODY_TAGS = setOf("ac:link-body", "ac:plain-text-link-body")

internal enum class Role {
    /** Formatting and other inline elements: only their text counts */
    TRANSPARENT,

    /** `ac:link` with body: text of the body counts */
    LINK,

    /** Inline element that is a single unit (inline macro, image, emoticon, link without body) */
    ATOM,

    /** `br` - single unit that counts as whitespace */
    BREAK,

    /** Element that contributes no text */
    SKIP,
}

internal fun roleOf(element: Element): Role {
    val name = element.normalName()
    return when {
        name == MARKER_TAG -> Role.TRANSPARENT
        name in BLOCK_TAGS || name in STRUCTURE_TAGS -> Role.SKIP
        name in MACRO_TAGS -> if (element.children().any { it.normalName() == "ac:rich-text-body" }) Role.SKIP else Role.ATOM
        name == ADF_EXTENSION -> if (element.getElementsByTag("ac:adf-content").isNotEmpty()) Role.SKIP else Role.ATOM
        name == "ac:link" -> if (linkBodies(element).any { it.text().isNotEmpty() }) Role.LINK else Role.ATOM
        name == "br" -> Role.BREAK
        name.startsWith("ri:") -> Role.SKIP
        name.contains(':') -> Role.ATOM
        name == "img" || name == "time" -> Role.ATOM
        else -> Role.TRANSPARENT
    }
}

internal fun linkBodies(link: Element): List<Element> = link.children().filter { it.normalName() in LINK_BODY_TAGS }

internal interface TextVisitor {
    fun text(node: TextNode) {}
    fun atom(node: Element, char: Char) {}
    fun enterMarker(marker: Element) {}
    fun exitMarker(marker: Element) {}
}

/** Walks children of [parent] in document order, reporting nodes that contribute to the flattened text. */
internal fun walkChildren(parent: Element, visitor: TextVisitor) {
    for (child in parent.childNodes().toList()) {
        walkNode(child, visitor)
    }
}

internal fun walkNode(node: Node, visitor: TextVisitor) {
    when (node) {
        is TextNode -> visitor.text(node)
        is Element -> when (roleOf(node)) {
            Role.TRANSPARENT -> {
                val marker = node.normalName() == MARKER_TAG
                if (marker) visitor.enterMarker(node)
                walkChildren(node, visitor)
                if (marker) visitor.exitMarker(node)
            }

            Role.LINK -> linkBodies(node).forEach { walkChildren(it, visitor) }
            Role.ATOM -> visitor.atom(node, OBJECT_CHAR)
            Role.BREAK -> visitor.atom(node, ' ')
            Role.SKIP -> {}
        }

        else -> {}
    }
}

/** Length of raw text the [node] contributes to its block */
internal fun rawLength(node: Node): Int {
    var length = 0
    walkNode(node, object : TextVisitor {
        override fun text(node: TextNode) {
            length += node.wholeText.length
        }

        override fun atom(node: Element, char: Char) {
            length += 1
        }
    })
    return length
}

internal class MarkerOccurrence(val element: Element, val rawStart: Int, val rawEnd: Int)

/**
 * Text block (paragraph, heading, list item, table cell, ...).
 *
 * @property kind tag name of the block. For containers with a single `<p>` it is the container name.
 * @property content element whose children make up block text
 * @property raw concatenated text of the block
 * @property norm [raw] with whitespace runs collapsed into single space and trimmed
 * @property normToRaw index in [raw] for every char of [norm]
 */
internal class TextBlock(
    val index: Int,
    val kind: String,
    val content: Element,
    val raw: String,
    val norm: String,
    val normToRaw: IntArray,
    val markers: List<MarkerOccurrence>,
) {
    val group: String = when (kind) {
        "h1", "h2", "h3", "h4", "h5", "h6" -> "heading"
        "td", "th", "caption", "figcaption" -> "cell"
        else -> "text"
    }

    val tokens: List<String> by lazy { tokenize(norm) }

    /** First index in [norm] whose raw position is at or after [rawPos] */
    fun normIndexAtOrAfter(rawPos: Int): Int {
        var lo = 0
        var hi = normToRaw.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (normToRaw[mid] < rawPos) lo = mid + 1 else hi = mid
        }
        return lo
    }

    /** Converts range in [norm] to range in [raw] */
    fun toRaw(start: Int, end: Int): Pair<Int, Int> = normToRaw[start] to normToRaw[end - 1] + 1

    override fun toString(): String = "$kind#$index: $norm"
}

private val TOKEN = Regex("""[\p{L}\p{N}_]+|[^\p{L}\p{N}_\s]""")

internal fun tokenize(text: String): List<String> = TOKEN.findAll(text.lowercase()).map { it.value }.toList()

internal fun isSpace(c: Char) = Character.isWhitespace(c) || Character.isSpaceChar(c)

/** Collects text blocks of the [root] in document order. Blocks with no text are skipped. */
internal fun collectBlocks(root: Element): List<TextBlock> {
    val result = mutableListOf<TextBlock>()
    collectInto(root, result)
    return result
}

private fun collectInto(element: Element, out: MutableList<TextBlock>) {
    for (child in element.children()) {
        val name = child.normalName()
        if (name == ADF_FALLBACK) continue
        if (name in BLOCK_TAGS) {
            val content = singleParagraph(child) ?: child
            val block = buildBlock(out.size, name, content)
            if (block.norm.isNotEmpty()) out += block
            collectInto(content, out)
        } else {
            collectInto(child, out)
        }
    }
}

private fun singleParagraph(container: Element): Element? {
    if (container.normalName() !in CONTAINER_TAGS) return null
    var paragraph: Element? = null
    for (node in container.childNodes()) {
        when (node) {
            is TextNode -> if (!node.isBlank) return null
            is Element -> {
                if (node.normalName() != "p" || paragraph != null) return null
                paragraph = node
            }
        }
    }
    return paragraph
}

private fun buildBlock(index: Int, kind: String, content: Element): TextBlock {
    val raw = StringBuilder()
    val markers = mutableListOf<MarkerOccurrence>()
    val openMarkers = mutableMapOf<Element, Int>()
    walkChildren(content, object : TextVisitor {
        override fun text(node: TextNode) {
            raw.append(node.wholeText)
        }

        override fun atom(node: Element, char: Char) {
            raw.append(char)
        }

        override fun enterMarker(marker: Element) {
            openMarkers[marker] = raw.length
        }

        override fun exitMarker(marker: Element) {
            markers += MarkerOccurrence(marker, openMarkers.remove(marker)!!, raw.length)
        }
    })
    val (norm, map) = normalize(raw)
    return TextBlock(index, kind, content, raw.toString(), norm, map, markers)
}

internal fun normalize(raw: CharSequence): Pair<String, IntArray> {
    val norm = StringBuilder()
    val map = IntArray(raw.length)
    var size = 0
    var pendingSpace = -1
    for (i in raw.indices) {
        val c = raw[i]
        if (isSpace(c)) {
            if (norm.isNotEmpty() && pendingSpace < 0) pendingSpace = i
        } else {
            if (pendingSpace >= 0) {
                norm.append(' ')
                map[size++] = pendingSpace
                pendingSpace = -1
            }
            norm.append(c)
            map[size++] = i
        }
    }
    return norm.toString() to map.copyOf(size)
}
