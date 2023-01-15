package com.github.zeldigas.text2confl.convert.markdown.diagram

import com.github.zeldigas.text2confl.convert.ConvertingContext
import com.github.zeldigas.text2confl.convert.markdown.MarkdownParser
import com.github.zeldigas.text2confl.convert.markdown.ext.AttributeRepositoryAware
import com.github.zeldigas.text2confl.convert.markdown.ext.replaceWith
import com.vladsch.flexmark.ast.FencedCodeBlock
import com.vladsch.flexmark.ast.Image
import com.vladsch.flexmark.ast.Paragraph
import com.vladsch.flexmark.ext.attributes.AttributeNode
import com.vladsch.flexmark.ext.attributes.AttributesExtension
import com.vladsch.flexmark.ext.attributes.AttributesNode
import com.vladsch.flexmark.ext.attributes.internal.NodeAttributeRepository
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.parser.block.NodePostProcessor
import com.vladsch.flexmark.parser.block.NodePostProcessorFactory
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.ast.NodeTracker
import com.vladsch.flexmark.util.data.DataKey
import com.vladsch.flexmark.util.data.MutableDataHolder
import com.vladsch.flexmark.util.misc.CharPredicate
import com.vladsch.flexmark.util.sequence.BasedSequence
import java.nio.file.Path


class DiagramsExtension : Parser.ParserExtension {

    companion object {
        val DIAGRAM_MAKERS = DataKey<DiagramMakers>("T2C_DIAGRAMS_PROVIDER", DiagramMakers.NOP)
    }

    override fun parserOptions(options: MutableDataHolder) {
    }

    override fun extend(builder: Parser.Builder) {
        builder.postProcessorFactory(DiagramsInjectingPostProcessorFactory())
    }
}

class DiagramsInjectingPostProcessorFactory : NodePostProcessorFactory(false) {

    init {
        addNodes(FencedCodeBlock::class.java)
    }

    override fun apply(document: Document): NodePostProcessor {
        return DiagramsInjectingPostProcessor(document)
    }
}

class DiagramsInjectingPostProcessor(document: Document) : NodePostProcessor(), AttributeRepositoryAware {
    companion object {
        private val DELIMITER = CharPredicate.anyOf(" \t")
    }

    private val attachmentsRegistry = MarkdownParser.ATTACHMENTS_REGISTRY[document]!!
    private val convertingContext: ConvertingContext = MarkdownParser.CONTEXT[document]!!
    private val location: Path? = MarkdownParser.DOCUMENT_LOCATION[document]
    private val diagramMakers: DiagramMakers = DiagramsExtension.DIAGRAM_MAKERS[document]
    override val nodeAttributeRepository: NodeAttributeRepository = AttributesExtension.NODE_ATTRIBUTES[document]

    override fun process(state: NodeTracker, node: Node) {
        if (node !is FencedCodeBlock) return

        val nodeInfo = node.info ?: return
        if (!(nodeInfo.isNotNull && nodeInfo.isNotBlank)) return

        val lang = node.getInfoDelimitedByAny(DELIMITER).unescape()

        val maker = diagramMakers.find(lang) ?: return
        val script = node.contentChars.normalizeEOL().trimEnd()
        val attributes = node.attributesMap

        val (attachment, imageInfo) = maker.toDiagram(
            script,
            attributes,
            location?.let { convertingContext.referenceProvider.pathFromDocsRoot(it) })

        attachmentsRegistry.register(attachment.resourceLocation.toString(), attachment)
        val paragraph = Paragraph()
        node.replaceWith(paragraph, state)
        val image = Image().apply {
            url = (BasedSequence.of(attachment.resourceLocation.toString()))

            imageInfo.title?.let { title = BasedSequence.of(it) }
        }
        paragraph.appendChild(image)
        nodeAttributeRepository.put(image, AttributesNode().apply {
            (attributes + imageInfo.attributes).map { (key, value) ->
                AttributeNode().also { attr ->
                    attr.name = BasedSequence.of(key)
                    attr.value = BasedSequence.of(value)
                }
            }.forEach { appendChild(it) }
        })
        state.nodeAddedWithChildren(image)
    }
}

