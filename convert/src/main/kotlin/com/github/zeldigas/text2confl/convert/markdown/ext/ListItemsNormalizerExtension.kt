package com.github.zeldigas.text2confl.convert.markdown.ext

import com.github.zeldigas.text2confl.convert.EditorVersion
import com.github.zeldigas.text2confl.convert.markdown.MarkdownParser
import com.vladsch.flexmark.ast.BulletListItem
import com.vladsch.flexmark.ast.ListItem
import com.vladsch.flexmark.ast.OrderedListItem
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.parser.block.NodePostProcessor
import com.vladsch.flexmark.parser.block.NodePostProcessorFactory
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.ast.NodeTracker
import com.vladsch.flexmark.util.data.MutableDataHolder

class ListItemsNormalizerExtension : Parser.ParserExtension {
    override fun parserOptions(options: MutableDataHolder?) {
        // No specific parser options needed
    }

    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.postProcessorFactory(object : NodePostProcessorFactory(false) {
            init {
                addNodes(OrderedListItem::class.java, BulletListItem::class.java)
            }

            override fun apply(document: Document): NodePostProcessor {
                return ListItemsNormalizer(document)
            }
        })
    }
}

internal class ListItemsNormalizer(document: Document) : NodePostProcessor() {
    val editorVersion = MarkdownParser.CONTEXT.get(document)?.conversionParameters?.editorVersion
    override fun process(state: NodeTracker, node: Node) {
        if (editorVersion != EditorVersion.V2 || node !is ListItem) return
        if (!node.hasOrMoreChildren(2)) return
        node.isTight = false
    }
}