package com.github.zeldigas.text2confl.convert.markdown.ext

import com.github.zeldigas.text2confl.convert.markdown.MarkdownConfiguration
import com.github.zeldigas.text2confl.convert.markdown.MarkdownParser
import com.github.zeldigas.text2confl.convert.markdown.addParameter
import com.github.zeldigas.text2confl.convert.markdown.macro
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

class SimpleMacroExtension : Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {

    override fun parserOptions(options: MutableDataHolder?) {
    }

    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.postProcessorFactory(object : NodePostProcessorFactory(false) {
            init {
                addNodes(LinkRef::class.java)
            }

            override fun apply(document: Document): NodePostProcessor {
                return SimpleMacroNodePostProcessor(document)
            }
        })
    }

    override fun rendererOptions(options: MutableDataHolder) {
    }

    override fun extend(htmlRendererBuilder: HtmlRenderer.Builder, rendererType: String) {
        htmlRendererBuilder.nodeRendererFactory { SimpleMacroRenderer() }
    }
}

class SimpleMacroNodePostProcessor(document: Document) : NodePostProcessor() {

    companion object {
        private val KEY = """\w+"""
        private val SIMPLE_VALUE = """[^\s"]+"""
        private val QUOTED_VALUE = """[^"]+?"""
        private val OPTION_REGEX = """(?<key>$KEY)=((?<value>$SIMPLE_VALUE)|"(?<quotedvalue>$QUOTED_VALUE)")""".toRegex()
        private val OPTION_UNNAMED_PATTERN = """$KEY=(($SIMPLE_VALUE)|"($QUOTED_VALUE)")"""
        private val OPTIONS_BLOCK = """$OPTION_UNNAMED_PATTERN(\s+$OPTION_UNNAMED_PATTERN)*""".toRegex()
    }

    private val referenceRepository: ReferenceRepository = Parser.REFERENCES.get(document)
    private val config: MarkdownConfiguration = MarkdownParser.PARSE_OPTIONS.get(document)!!

    override fun process(state: NodeTracker, node: Node) {
        if (node !is LinkRef) return

        if (node.isDefined || node.getReferenceNode(referenceRepository) != null) {
            return
        }
        val reference = node.reference.toString().trim()
        if (!reference.contains(' ')) {
            return
        }
        val (name, options) = reference.split(' ', limit = 2)
        if (validMacroName(name) && validOptionsString(options)) {
            val macroName = name.lowercase()
            val parameters = extractMacroParameters(options)
            node.replaceWith(ConfluenceSimpleMacroNode(macroName, parameters), state)
        }
    }

    private fun validMacroName(name: String): Boolean =
        config.parseAnyMacro || config.supportedMacros.any { name.equals(it, ignoreCase = true) }

    private fun validOptionsString(options: String): Boolean = OPTIONS_BLOCK.matches(options.trim())

    private fun extractMacroParameters(options: String): Map<String, String> =
        OPTION_REGEX.findAll(options).map {
            it.groups["key"]?.value!! to (it.groups["value"] ?: it.groups["quotedvalue"])!!.value
        }.toMap()
}

class SimpleMacroRenderer : NodeRenderer {
    override fun getNodeRenderingHandlers(): Set<NodeRenderingHandler<*>> {
        return setOf(
            NodeRenderingHandler(ConfluenceSimpleMacroNode::class.java, this::render)
        )
    }

    private fun render(node: ConfluenceSimpleMacroNode, @Suppress("UNUSED_PARAMETER") context: NodeRendererContext, html: HtmlWriter) {
        val params = node.parameters.entries.sortedBy { it.key }
        html.macro(node.name) {
            params.forEach { (name, value) -> addParameter(name, value) }
        }
    }
}

class ConfluenceSimpleMacroNode(val name: String, val parameters: Map<String, String>) : Node() {

    override fun getSegments(): Array<BasedSequence> {
        return EMPTY_SEGMENTS
    }
}