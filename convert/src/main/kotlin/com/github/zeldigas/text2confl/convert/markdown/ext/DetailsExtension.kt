package com.github.zeldigas.text2confl.convert.markdown

import com.vladsch.flexmark.ast.HtmlBlock
import com.vladsch.flexmark.ext.admonition.AdmonitionBlock
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.parser.block.NodePostProcessor
import com.vladsch.flexmark.parser.block.NodePostProcessorFactory
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.ast.NodeTracker
import com.vladsch.flexmark.util.data.MutableDataHolder
import com.vladsch.flexmark.util.sequence.BasedSequence
import org.jsoup.Jsoup

class DetailsExtension : Parser.ParserExtension {

    override fun parserOptions(options: MutableDataHolder) {
    }

    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.postProcessorFactory(DetailsFactory())
    }
}

private class DetailsFactory : NodePostProcessorFactory(false) {

    init {
        addNodes(HtmlBlock::class.java)
    }

    override fun apply(document: Document): NodePostProcessor {
        return DetailsPostProcessor()
    }
}

private class DetailsPostProcessor : NodePostProcessor() {

    override fun process(state: NodeTracker, node: Node) {
        if (node !is HtmlBlock) return

        if (!node.chars.startsWith("<details")) {
            return
        }
        val title = extractDetailsSummary(node)
        val content = mutableListOf<Node>()
        var endNode: Node? = null
        var next: Node? = node.next
        while (next != null) {
            if (next is HtmlBlock && next.chars.trim().equals("</details>")) {
                endNode = next
                break
            } else {
                content.add(next)
            }
            next = next.next
        }

        if (endNode != null) {
            val expandBlock = AdmonitionBlock()
            expandBlock.info = BasedSequence.of("expand")
            title?.let { expandBlock.title = BasedSequence.of(it) }
            content.forEach {
                it.unlink()
                state.nodeRemoved(it)
                expandBlock.appendChild(it)
            }
            node.insertBefore(expandBlock)
            state.nodeAddedWithChildren(expandBlock)
            listOf(node, endNode).forEach { it.unlink(); state.nodeRemoved(it) }
        }
    }

    private fun extractDetailsSummary(node: HtmlBlock): String? {
        val doc = Jsoup.parseBodyFragment("${node.chars}</details>")
        val summary = doc.body().getElementsByTag("summary")
        return summary.first()?.text()
    }
}