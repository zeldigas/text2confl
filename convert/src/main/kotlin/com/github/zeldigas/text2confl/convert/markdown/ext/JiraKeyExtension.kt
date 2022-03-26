package com.github.zeldigas.text2confl.convert.markdown.ext

import com.vladsch.flexmark.ast.LinkRef
import com.vladsch.flexmark.ast.util.ReferenceRepository
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.HtmlWriter
import com.vladsch.flexmark.html.renderer.NodeRenderer
import com.vladsch.flexmark.html.renderer.NodeRendererContext
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.parser.block.NodePostProcessor
import com.vladsch.flexmark.parser.block.NodePostProcessorFactory
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.ast.NodeTracker
import com.vladsch.flexmark.util.data.MutableDataHolder
import com.vladsch.flexmark.util.sequence.BasedSequence

class JiraKeyExtension : Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {

    override fun parserOptions(options: MutableDataHolder?) {
    }

    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.postProcessorFactory(object : NodePostProcessorFactory(false) {
            init {
                addNodes(LinkRef::class.java)
            }

            override fun apply(document: Document): NodePostProcessor {
                return JiraKeyPostProcessor(document)
            }
        })
    }

    override fun rendererOptions(options: MutableDataHolder) {
    }

    override fun extend(htmlRendererBuilder: HtmlRenderer.Builder, rendererType: String) {
        htmlRendererBuilder.nodeRendererFactory { JiraKeyRenderer() }
    }
}

private const val JIRA_PREFIX = "JIRA:"

class JiraKeyPostProcessor(document: Document) : NodePostProcessor() {

    private val referenceRepository: ReferenceRepository = Parser.REFERENCES.get(document)

    override fun process(state: NodeTracker, node: Node) {
        if (node !is LinkRef) return

        if (node.isDefined || node.getReferenceNode(referenceRepository) != null) {
            return
        }
        val reference = node.reference.toString()
        if (reference.startsWith(JIRA_PREFIX)) {
            node.replaceWith(JiraRefNode(reference.substring(JIRA_PREFIX.length)), state)
        }
    }
}

class JiraKeyRenderer : NodeRenderer {
    override fun getNodeRenderingHandlers(): Set<NodeRenderingHandler<*>> {
        return setOf(
            NodeRenderingHandler(JiraRefNode::class.java, this::render)
        )
    }

    private fun render(node: JiraRefNode, context: NodeRendererContext, html: HtmlWriter) {
        html.raw("""<ac:structured-macro ac:name="jira"><ac:parameter ac:name="key">${node.key}</ac:parameter></ac:structured-macro>""")
    }
}

class JiraRefNode(val key: String) : Node() {

    override fun getSegments(): Array<BasedSequence> {
        return EMPTY_SEGMENTS
    }
}