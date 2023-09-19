package com.github.zeldigas.text2confl.convert.markdown.export

import com.vladsch.flexmark.html2md.converter.*
import com.vladsch.flexmark.html2md.converter.internal.HtmlConverterCoreNodeRenderer
import com.vladsch.flexmark.util.data.DataHolder
import com.vladsch.flexmark.util.sequence.LineAppendable
import com.vladsch.flexmark.util.sequence.RepeatedSequence
import io.ktor.util.*
import org.jsoup.nodes.Element

class ConfluenceCustomNodeRenderer(options: DataHolder) : HtmlNodeRenderer {

    private val myHtmlConverterOptions = HtmlConverterOptions(options)
    private val linkResolver = HtmlToMarkdownConverter.LINK_RESOLVER.get(options)

    private val basicRenderer =
        HtmlConverterCoreNodeRenderer(options).htmlNodeRendererHandlers.map { it.tagName to it }.toMap()

    companion object {
        private val SIMPLE_CELL_TAGS =
            setOf("code", "b", "strong", "a", "br", "strike", "em", "i", "ins", "sub", "sup", "pre")
    }

    override fun getHtmlNodeRendererHandlers(): Set<HtmlNodeRendererHandler<*>> {
        return setOf(
            HtmlNodeRendererHandler("ac:structured-macro", Element::class.java, this::processMacro),
            HtmlNodeRendererHandler("ac:task-list", Element::class.java, this::processTaskList),
            HtmlNodeRendererHandler("ac:link", Element::class.java, this::processLink),
            HtmlNodeRendererHandler("ac:image", Element::class.java, this::processImage),
            HtmlNodeRendererHandler("time", Element::class.java, this::html),
            HtmlNodeRendererHandler(FlexmarkHtmlConverter.TABLE_NODE, Element::class.java, this::processTable),
            HtmlNodeRendererHandler(FlexmarkHtmlConverter.EM_NODE, Element::class.java, this::processEmphasis),
            HtmlNodeRendererHandler(FlexmarkHtmlConverter.STRONG_NODE, Element::class.java, this::processBold),
        )
    }

    private fun html(element: Element, context: HtmlNodeConverterContext, writer: HtmlMarkdownWriter) {
        writer.append(element.toString())
    }

    private fun processEmphasis(element: Element, context: HtmlNodeConverterContext, writer: HtmlMarkdownWriter) {
        context.wrapTextNodes(element, "*", false)
    }

    private fun processBold(element: Element, context: HtmlNodeConverterContext, writer: HtmlMarkdownWriter) {
        context.wrapTextNodes(element, "**", false)
    }

    private fun processMacro(
        element: Element,
        context: HtmlNodeConverterContext,
        out: HtmlMarkdownWriter
    ) {
        when (element.attr("ac:name")) {
            "anchor" -> Unit //no handling for anchors
            "code" -> generateCodeBlock(element, out)
            in listOf("note", "tip", "warning", "info", "expand") -> generateAdmonition(element, context, out)
            "status" -> generateStatus(element, out)

            else -> {
                if (isSimpleMacro(element)) {
                    generateSimpleMacros(element, out)
                } else {
                    out.append("<!-- ").append(element.toString()).append(" -->")
                }
            }
        }
    }

    private fun generateStatus(element: Element, out: HtmlMarkdownWriter) {
        val attributes = macroParameters(element)
        out.append("<status color=\"").append(attributes["colour"]?.lowercase()).append("\">")
        out.append(attributes["title"])
        out.append("</status>")
    }

    private fun isSimpleMacro(element: Element) = element.children().all { it.tagName() == "ac:parameter" }

    private fun generateSimpleMacros(element: Element, out: HtmlMarkdownWriter) {
        val attributes = macroParameters(element)
        out.append("[").append(element.attr("ac:name").toUpperCasePreservingASCIIRules())
        if (attributes.isNotEmpty()) {
            out.append(" ")
            generateAttributes(out, attributes)
        }
        out.append("]")
    }

    private fun generateCodeBlock(element: Element, out: HtmlMarkdownWriter) {
        val attributes = element.getElementsByTag("ac:parameter").map { it.attr("ac:name") to it.ownText() }.toMap()
        val script = element.getElementsByTag("ac:plain-text-body").text()

        out.blankLine().append("```")
        if ("language" in attributes) {
            out.append(attributes["language"])
        }
        val otherAttributes = attributes - "language"
        if (otherAttributes.isNotEmpty()) {
            out.append(" {")
            generateAttributes(out, otherAttributes)
            out.append("}")
        }
        out.line()
        out.openPreFormatted(true)
        out.append(script)
        out.closePreFormatted()
        out.line().append("```").line()
        out.tailBlankLine()
    }

    fun generateAdmonition(element: Element, context: HtmlNodeConverterContext, out: HtmlMarkdownWriter) {
        if (element.previousElementSibling() != null) {
            out.blankLine()
        }
        out.append("!!! ").append(element.attr("ac:name"))
        val attributes = macroParameters(element)
        if ("title" in attributes) {
            out.append(" \"")
            out.append(context.escapeSpecialChars(attributes["title"]!!))
            out.append("\"")
        }
        out.blankLine()
        out.pushPrefix()
        out.addPrefix("    ")
        context.renderChildren(
            element.childNodes().filterIsInstance<Element>().first { it.tagName() == "ac:rich-text-body" }, true, null
        )
        out.popPrefix()
    }

    private fun macroParameters(element: Element) =
        element.childNodes().filterIsInstance<Element>().filter { it.tagName() == "ac:parameter" }
            .map { it.attr("ac:name") to it.ownText() }.toMap()

    private fun generateAttributes(out: HtmlMarkdownWriter, attributes: Map<String, String>) {
        var counter = attributes.size
        for ((key, value) in attributes.entries) {
            counter--
            out.append(key).append("=")
            val attrValue = if (value.any { it.isWhitespace() }) "\"$value\"" else value
            out.append(attrValue)
            if (counter > 0) {
                out.append(" ")
            }
        }
    }


    private fun processTaskList(element: Element, context: HtmlNodeConverterContext, writer: HtmlMarkdownWriter) {
        writer.blankLine()
        element.childNodes().filterIsInstance<Element>().filter { it.tagName() == "ac:task" }.forEach {
            handleTaskListItem(context, writer, it)
        }
    }

    private fun handleTaskListItem(
        context: HtmlNodeConverterContext,
        out: HtmlMarkdownWriter,
        item: Element
    ) {
        context.pushState(item)
        val itemPrefix: CharSequence = if (item.firstElementChild()!!.text() == "incomplete") "* [ ] " else "* [x] "
        val count = myHtmlConverterOptions.listItemIndent
        val childPrefix = RepeatedSequence.repeatOf(" ", count)
        out.line().append(itemPrefix)
        out.pushPrefix()
        out.addPrefix(childPrefix, true)
        val offset = out.offsetWithPending()
        context.renderChildren(item.child(1), true, null)
        if (offset == out.offsetWithPending()) {
            // completely empty, add space and make sure it is not suppressed
            val options = out.options
            out.setOptions(options and (LineAppendable.F_TRIM_TRAILING_WHITESPACE or LineAppendable.F_TRIM_LEADING_WHITESPACE).inv())
            out.line()
            out.setOptions(options)
        } else {
            out.line()
        }
        out.popPrefix()
        context.popState(out)
    }

    private fun processLink(element: Element, context: HtmlNodeConverterContext, writer: HtmlMarkdownWriter) {
        if (element.getElementsByTag("ri:page").isNotEmpty()) {
            processLinkToPage(element, context, writer)
        } else if (element.getElementsByTag("ri:attachment").isNotEmpty()) {
            processLinkToAttachment(element, context, writer)
        } else if (element.hasAttr("ac:anchor")) {
            processThisPageAnchor(element, context, writer)
        }
    }

    private fun processLinkToPage(element: Element, context: HtmlNodeConverterContext, writer: HtmlMarkdownWriter) {
        val pageNode = element.getElementsByTag("ri:page").first()!!
        val title = pageNode.attr("ri:content-title")
        generateLinkName(element, context, writer) { title }
        val linkToPage = linkResolver.resolve(pageNode.attr("ri:space-key").ifEmpty { null }, title)
        writer.append("(").append(linkToPage)
        if (element.hasAttr("ac:anchor")) {
            writer.append("#").append(element.attr("ac:anchor"))
        }
        writer.append(")")
    }

    private fun processLinkToAttachment(
        element: Element,
        context: HtmlNodeConverterContext,
        writer: HtmlMarkdownWriter
    ) {
        generateLinkName(element, context, writer)
        writer.append("[")
        val attachmentName = element.getElementsByTag("ri:attachment").first()!!.attr("ri:filename")
        writer.append(attachmentName.toString())
        writer.append("]")
    }


    private fun processThisPageAnchor(element: Element, context: HtmlNodeConverterContext, writer: HtmlMarkdownWriter) {
        generateLinkName(element, context, writer)
        writer.append("(#").append(element.attr("ac:anchor")).append(")")
    }

    private fun generateLinkName(
        element: Element,
        context: HtmlNodeConverterContext,
        writer: HtmlMarkdownWriter,
        fallback: () -> String? = { null }
    ) {
        writer.append("[")
        val plainText = element.getElementsByTag("ac:plain-text-link-body").firstOrNull()
        if (plainText != null) {
            writer.append(plainText.ownText())
        } else if (element.getElementsByTag("ac:link-body").first() != null) {
            val first = element.getElementsByTag("ac:link-body").first()!!
            context.renderChildren(first, true, null)
        } else {
            writer.append(
                fallback()
                    ?: throw IllegalStateException("$element does not have link body and fallback also did not provide link name")
            )
        }
        writer.append("]")
    }

    private fun processImage(element: Element, context: HtmlNodeConverterContext, writer: HtmlMarkdownWriter) {
        val attributes = element.attributes().map { it.key.removePrefix("ac:") to it.value }.toMap()
        writer.append("![")
        attributes["alt"]?.let { writer.append(it) }
        writer.append("]")
        if (isAttachment(element)) {
            writer.append("[")
            writer.append(element.getElementsByTag("ri:attachment").first()!!.attr("ri:filename"))
            writer.append("]")
        } else {
            writer.append("(")
            writer.append(element.getElementsByTag("ri:url").first()!!.attr("ri:value"))
            if ("title" in attributes) {
                writer.append(" \"").append(context.prepareText(attributes.getValue("title"))).append("\"")
            }
            writer.append(")")
        }
        val otherAttributes = attributes - listOf("alt", "title")
        if (otherAttributes.isNotEmpty()) {
            writer.append("{")
            generateAttributes(writer, otherAttributes)
            writer.append("}")
        }
    }

    private fun isAttachment(element: Element): Boolean = element.getElementsByTag("ri:url").first() == null

    private fun processTable(element: Element, context: HtmlNodeConverterContext, writer: HtmlMarkdownWriter) {
        if (isSimpleTable(element)) {
            basicRenderer.getValue(FlexmarkHtmlConverter.TABLE_NODE).render(element, context, writer)
        } else {
            renderTable(element, context, writer)
        }
    }

    private fun isSimpleTable(table: Element): Boolean {
        return headerIsSimple(table.getElementsByTag("thead").first())
                && bodyIsSimple(table.getElementsByTag("tbody").first())
    }

    private fun bodyIsSimple(bodyOrNull: Element?): Boolean {
        val body = bodyOrNull ?: return true

        if (body.getElementsByTag("td").any { isComplexCell(it) }) return false

        val headerCells = body.getElementsByTag("th")
        if (headerCells.isEmpty()) return true
        if (headerCells.any { isComplexCell(it) }) return false

        for (row in body.getElementsByTag("tr")) {
            val tags = row.children().map { it.tagName() }.toSet()
            if ("th" in tags && "td" in tags) return false
        }

        return true
    }

    private fun headerIsSimple(headOrNull: Element?): Boolean {
        val header = headOrNull ?: return true

        return header.getElementsByTag("th").none { isComplexCell(it) }
    }

    private fun isComplexCell(it: Element): Boolean {
        val children = it.childNodes().filterIsInstance<Element>()
        if (children.isEmpty()) return false
        if (children.map { it.tagName() }.all { it in SIMPLE_CELL_TAGS }) return false
        val complexCell = children.size > 1 || children[0].text().lines().size > 1
        return complexCell
    }

    private fun renderTable(element: Element, context: HtmlNodeConverterContext, writer: HtmlMarkdownWriter) {
        writer.blankLine().append("<table>").line()
        for (node in element.childNodes()) {
            if (node is Element) {
                when (node.tagName()) {
                    "thead" -> renderTableHead(node, context, writer)
                    "tbody" -> renderTableBody(node, context, writer)
                    else -> context.render(node)
                }
            } else {
                context.render(node)
            }
        }
        writer.append("</table>").blankLine()
    }

    private fun renderTableHead(node: Element, context: HtmlNodeConverterContext, writer: HtmlMarkdownWriter) {
        renderTableSection("thead", node, context, writer)
    }

    private fun renderTableBody(node: Element, context: HtmlNodeConverterContext, writer: HtmlMarkdownWriter) {
        renderTableSection("tbody", node, context, writer)
    }

    private fun renderTableSection(
        tag: String,
        node: Element,
        context: HtmlNodeConverterContext,
        writer: HtmlMarkdownWriter
    ) {
        writer.append("<$tag>").line()
        for (node in node.childNodes()) {
            if (node is Element) {
                when (node.tagName()) {
                    "tr" -> renderTableRow(node, context, writer)
                    else -> context.render(node)
                }
            } else {
                context.render(node)
            }
        }
        writer.append("</$tag>").line()
    }

    private fun renderTableRow(node: Element, context: HtmlNodeConverterContext, writer: HtmlMarkdownWriter) {
        tagWithAttributes("tr", node, writer)
        writer.line()
        for (node in node.childNodes()) {
            if (node is Element) {
                when (node.tagName()) {
                    "th" -> renderCell("th", node, context, writer)
                    "td" -> renderCell("td", node, context, writer)
                    else -> context.render(node)
                }
            } else {
                context.render(node)
            }
        }
        writer.append("</tr>").line()
    }

    private fun tagWithAttributes(tag: String, node: Element, writer: HtmlMarkdownWriter) {
        writer.append("<").append(tag)
        if (!node.attributes().isEmpty) {
            writer.append(" ").append(node.attributes().html())
        }
        writer.append(">")
    }

    private fun renderCell(tag: String, node: Element, context: HtmlNodeConverterContext, writer: HtmlMarkdownWriter) {
        tagWithAttributes(tag, node, writer)
        if (node.children().any { it is Element }) {
            writer.blankLine()
            context.renderChildren(node, true, null)
            writer.line()
        } else {
            context.renderChildren(node, true, null)
        }

        writer.append("</").append(tag).append(">").line()
    }
}
