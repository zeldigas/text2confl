package com.github.zeldigas.text2confl.convert.markdown

import com.vladsch.flexmark.ast.HtmlInline
import com.vladsch.flexmark.ast.Text
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.parser.block.NodePostProcessor
import com.vladsch.flexmark.parser.block.NodePostProcessorFactory
import com.vladsch.flexmark.util.ast.DoNotDecorate
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.ast.NodeTracker
import com.vladsch.flexmark.util.data.MutableDataHolder
import com.vladsch.flexmark.util.sequence.BasedSequence

class StatusExtension : Parser.ParserExtension {

    override fun parserOptions(options: MutableDataHolder) {
    }

    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.postProcessorFactory(Factory())
    }
}

class Factory : NodePostProcessorFactory(false) {

    init {
        addNodes(HtmlInline::class.java)
    }

    override fun apply(document: Document): NodePostProcessor {
        return StatusPostProcessor()
    }
}

class StatusPostProcessor : NodePostProcessor() {
    companion object {
        val PATTERN = """<status\s+colou?r="(\w+)"\s*>""".toRegex()
    }

    override fun process(state: NodeTracker, node: Node) {
        if (node !is HtmlInline) return
        val match = PATTERN.matchEntire(node.chars) ?: return
        val expectedText: Text = node.next as? Text ?: return
        val expectedCloseTag: HtmlInline = expectedText.next as? HtmlInline ?: return

        if (expectedCloseTag.chars.toString() != "</status>") return

        val statusBlock = ConfluenceStatusNode(match.groupValues[1], expectedText.chars.toString())
        node.insertBefore(statusBlock)

        state.nodeAdded(statusBlock)
        listOf(node, expectedText, expectedCloseTag).forEach { it.unlink(); state.nodeRemoved(it) }
    }
}

class ConfluenceStatusNode(val color: String, val text: String) : Node(), DoNotDecorate {
    override fun getSegments(): Array<BasedSequence> {
        return EMPTY_SEGMENTS
    }
}