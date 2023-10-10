package com.github.zeldigas.text2confl.convert.markdown

import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.ConvertingContext
import com.github.zeldigas.text2confl.convert.confluence.Anchor
import com.github.zeldigas.text2confl.convert.confluence.Xref
import com.github.zeldigas.text2confl.convert.markdown.ext.AttributeRepositoryAware
import com.vladsch.flexmark.ast.*
import com.vladsch.flexmark.ext.admonition.AdmonitionBlock
import com.vladsch.flexmark.ext.attributes.AttributesExtension
import com.vladsch.flexmark.ext.attributes.internal.NodeAttributeRepository
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListItem
import com.vladsch.flexmark.ext.toc.TocBlock
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.HtmlRenderer.HtmlRendererExtension
import com.vladsch.flexmark.html.HtmlRendererOptions
import com.vladsch.flexmark.html.HtmlWriter
import com.vladsch.flexmark.html.renderer.*
import com.vladsch.flexmark.parser.ListOptions
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.parser.block.NodePostProcessor
import com.vladsch.flexmark.parser.block.NodePostProcessorFactory
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.ast.NodeTracker
import com.vladsch.flexmark.util.ast.TextCollectingVisitor
import com.vladsch.flexmark.util.data.DataHolder
import com.vladsch.flexmark.util.data.DataKey
import com.vladsch.flexmark.util.data.MutableDataHolder
import com.vladsch.flexmark.util.sequence.BasedSequence
import com.vladsch.flexmark.util.sequence.Escaping
import io.github.oshai.kotlinlogging.KotlinLogging


internal class ConfluenceFormatExtension : HtmlRendererExtension, Parser.ParserExtension {

    companion object {
        val ATTACHMENTS = DataKey<Map<String, Attachment>>("FENCED_CODE_CONTENT_BLOCK", emptyMap())
    }

    override fun rendererOptions(options: MutableDataHolder) {
    }

    override fun extend(htmlRendererBuilder: HtmlRenderer.Builder, rendererType: String) {
        htmlRendererBuilder.nodeRendererFactory { ConfluenceNodeRenderer(it) }
    }

    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.postProcessorFactory(ConfluenceRawTagsFactory())
    }

    override fun parserOptions(options: MutableDataHolder) {
    }
}

class ConfluenceRawTagsFactory : NodePostProcessorFactory(false) {

    init {
        addNodes(HtmlBlock::class.java, HtmlInline::class.java)
    }

    override fun apply(document: Document): NodePostProcessor {
        return ConfluenceRawTagsPostProcessor()
    }
}

class ConfluenceRawTagsPostProcessor : NodePostProcessor() {

    companion object {
        val REPLACEMENTS = mapOf("<ac-" to "<ac:", "</ac-" to "</ac:", "<ri-" to "<ri:", "</ri-" to "</ri:")
    }

    override fun process(state: NodeTracker, node: Node) {
        if (!(node is HtmlBlock || node is HtmlInline)) return

        val replacement = REPLACEMENTS.keys.firstOrNull { node.chars.startsWith(it) } ?: return
        node.chars = node.chars.replace(0, replacement.length, REPLACEMENTS.getValue(replacement))
    }
}

/**
 * Confluence storage format customizations that override [com.vladsch.flexmark.html.renderer.CoreNodeRenderer]
 */
class ConfluenceNodeRenderer(options: DataHolder) : PhasedNodeRenderer, AttributeRepositoryAware {

    companion object {
        private val logger = KotlinLogging.logger { }

        val ALLOWED_TOC_ATTRIBUTES =
            setOf("maxLevel", "minLevel", "include", "exclude", "style", "class", "separator", "type", "outline")
        private val OPTIONS_ITEM_REGEX = """(?<key>\w+)=((?<value>[^\s"]+)|"(?<quotedvalue>[^"]+?)")""".toRegex()
        val ALLOWED_IMAGE_ATTRIBUTES = setOf(
            "align",
            "border",
            "class",
            "title",
            "style",
            "thumbnail",
            "alt",
            "height",
            "width",
            "vspace",
            "hspace",
            "queryparams"
        )
        val ALLOWED_CODE_ATTRIBUTES = setOf("title", "collapse", "linenumbers", "firstline", "theme")
        val ADMONITION_TYPES = setOf("tip", "note", "info", "warning", "expand")
        const val DEFAULT_ADMONITION_TYPE = "note"
    }

    private val sourcePath = MarkdownParser.DOCUMENT_LOCATION[options]!!
    private val referenceRepository = Parser.REFERENCES.get(options)
    private val recheckUndefinedReferences = HtmlRenderer.RECHECK_UNDEFINED_REFERENCES.get(options)
    private val attachments: Map<String, Attachment> = ConfluenceFormatExtension.ATTACHMENTS[options]
    private val convertingContext: ConvertingContext = MarkdownParser.CONTEXT[options]!!
    private val listOptions = ListOptions.get(options)
    override val nodeAttributeRepository: NodeAttributeRepository = AttributesExtension.NODE_ATTRIBUTES.get(options)

    override fun getNodeRenderingHandlers(): Set<NodeRenderingHandler<*>> {
        return setOf(
            NodeRenderingHandler(FencedCodeBlock::class.java, this::render),
            NodeRenderingHandler(Image::class.java, this::render),
            NodeRenderingHandler(ImageRef::class.java, this::render),
            NodeRenderingHandler(Link::class.java, this::render),
            NodeRenderingHandler(LinkRef::class.java, this::render),
            NodeRenderingHandler(Heading::class.java, this::render),
            NodeRenderingHandler(OrderedList::class.java, this::render),
            NodeRenderingHandler(BulletList::class.java, this::render),
            NodeRenderingHandler(TaskListItem::class.java, this::render),
            NodeRenderingHandler(TocBlock::class.java, this::render),
            NodeRenderingHandler(AdmonitionBlock::class.java, this::render),
            NodeRenderingHandler(ConfluenceStatusNode::class.java, this::render),
            NodeRenderingHandler(ConfluenceUserNode::class.java, this::render),
        )
    }

    override fun getRenderingPhases(): Set<RenderingPhase> {
        return setOf(RenderingPhase.BODY_TOP)
    }

    override fun renderDocument(
        context: NodeRendererContext,
        html: HtmlWriter,
        document: Document,
        phase: RenderingPhase
    ) {
        if (phase != RenderingPhase.BODY_TOP) return
        if (!convertingContext.conversionParameters.addAutogeneratedNote) return

        val text = convertingContext.conversionParameters.let { params ->
            val pathFromRoot = convertingContext.referenceProvider.pathFromDocsRoot(sourcePath)
            params.noteText.replace("__doc-root__", params.docRootLocation).replace("__file__", pathFromRoot.toString())
        }

        html.tag("p") {
            html.raw("<ac:structured-macro ac:name=\"note\"><ac:rich-text-body><p>$text</p></ac:rich-text-body></ac:structured-macro>")
        }
    }

    private fun render(node: FencedCodeBlock, context: NodeRendererContext, html: HtmlWriter) {
        html.line()

        val info: BasedSequence = node.info
        val htmlOptions: HtmlRendererOptions = context.htmlOptions
        val hasLanguageTag = info.isNotNull && !info.isBlank

        html.openTag("ac:structured-macro", mapOf("ac:name" to "code")).openPre()
        val language: String? = if (hasLanguageTag) {
            node.getInfoDelimitedByAny(htmlOptions.languageDelimiterSet).unescape().let { lang ->
                convertingContext.languageMapper.mapToConfluenceLanguage(lang)
            }
        } else {
            null
        }
        if (language != null) {
            html.addParameter("language", language)
        }
        node.attributesMap.filter { (key, value) -> key in ALLOWED_CODE_ATTRIBUTES && value.isNotBlank() }
            .forEach { (key, value) -> html.addParameter(key, value) }
        html.tagWithCData("ac:plain-text-body", node.contentChars.normalizeEOL().trimEnd())
        html.closeTag("ac:structured-macro")
    }

    private fun render(node: Image, context: NodeRendererContext, html: HtmlWriter) {
        val attributes = node.attributesMap + mapOf(
            "alt" to imageAltText(node),
            "title" to imageTitle(node)
        )
        renderImage(html, node.url.unescape(), attributes) { buildUrl(context, node) }
    }

    private fun buildUrl(
        context: NodeRendererContext,
        node: Image
    ): String {
        //code taken from standard renderer
        val resolvedLink = context.resolveLink(LinkType.IMAGE, node.url.unescape(), null, null)
        var linkUrl: String = resolvedLink.url

        if (!node.urlContent.isEmpty()) {
            // reverse URL encoding of =, &
            val content = Escaping.percentEncodeUrl(node.urlContent).replace("+", "%2B").replace("%3D", "=")
                .replace("%26", "&amp;")
            linkUrl += content
        }
        return linkUrl
    }

    private fun render(node: ImageRef, context: NodeRendererContext, html: HtmlWriter) {
        if (!node.isDefined && recheckUndefinedReferences) {
            if (node.getReferenceNode(referenceRepository) != null) {
                node.isDefined = true
            }
        }

        if (!node.isDefined) {
            // empty ref, we treat it as text
            html.text(node.chars.unescape())
        } else {
            val reference = node.getReferenceNode(referenceRepository)
            val attributes = node.attributesMap + mapOf(
                "alt" to imageAltText(node),
                "title" to imageTitle(reference)
            )
            renderImage(
                html,
                reference.reference.toString(),
                attributes
            ) { context.resolveLink(LinkType.IMAGE, reference.url.unescape(), null).url }
        }

    }

    private fun imageTitle(node: LinkNodeBase) =
        if (node.title.isNotNull) node.title.unescape() else null

    private fun imageAltText(node: Node) = TextCollectingVisitor().collectAndGetText(node)

    private fun renderImage(
        html: HtmlWriter,
        attachmentReference: String,
        attributes: Map<String, String?>,
        externalUrlProvider: () -> String
    ) {
        val imageAttributes = attributes
            .filter { (key, value) -> key in ALLOWED_IMAGE_ATTRIBUTES && value != null && value.isNotBlank() }
            .map { (key, v) -> "ac:$key" to v!! }
            .toMap()

        html.openTag("ac:image", imageAttributes)
        if (attachmentReference in attachments) {
            html.voidTag(
                "ri:attachment",
                mapOf("ri:filename" to attachments.getValue(attachmentReference).attachmentName)
            )
        } else {
            html.voidTag("ri:url", mapOf("ri:value" to externalUrlProvider()))
        }
        html.closeTag("ac:image")
    }

    private fun render(node: Link, context: NodeRendererContext, html: HtmlWriter) {
        val url = node.url.unescape()
        renderLink(url, url, html, node, context) {
            if (node.text != null) node.text.normalizeEOL().trimEnd() else null
        }
    }

    private fun <T : Node> renderLink(
        url: String,
        attachmentReference: String,
        html: HtmlWriter,
        node: T,
        context: NodeRendererContext,
        textExtractor: (T) -> String?
    ) {
        val xref = convertingContext.referenceProvider.resolveReference(sourcePath, url)
        if (xref != null) {
            when (xref) {
                is Xref -> {
                    html.openTag("ac:link", buildMap { xref.anchor?.let { put("ac:anchor", it) } })
                    html.voidTag(
                        "ri:page",
                        mapOf("ri:content-title" to xref.target, "ri:space-key" to convertingContext.targetSpace)
                    )
                    appendLinkBody(node, html, context, textExtractor)
                    html.closeTag("ac:link")
                }

                is Anchor -> {
                    html.openTag("ac:link", mapOf("ac:anchor" to xref.target))
                    appendLinkBody(node, html, context, textExtractor)
                    html.closeTag("ac:link")
                }
            }
        } else if (attachmentReference in attachments) {
            html.openTag("ac:link")
            html.voidTag(
                "ri:attachment",
                mapOf("ri:filename" to attachments.getValue(attachmentReference).attachmentName)
            )
            appendLinkBody(node, html, context, textExtractor)
            html.closeTag("ac:link")
        } else {
            context.delegateRender()
        }
    }

    private fun <T : Node> appendLinkBody(
        node: T,
        html: HtmlWriter,
        context: NodeRendererContext,
        textExtractor: (T) -> String?
    ) {
        if (node.withRichFormatting) {
            html.tag("ac:link-body")
            context.renderChildren(node)
            html.closeTag("ac:link-body")
        } else {
            val text = textExtractor(node)
            if (text != null) {
                html.tagWithCData("ac:plain-text-link-body", text)
            }
        }
    }

    private fun render(node: LinkRef, context: NodeRendererContext, html: HtmlWriter) {
        if (!node.isDefined && recheckUndefinedReferences) {
            if (node.getReferenceNode(referenceRepository) != null) {
                node.isDefined = true
            }
        }
        if (!node.isDefined) {
            context.delegateRender()
        } else {
            val reference = node.getReferenceNode(referenceRepository)!!
            val resolvedLink = context.resolveLink(LinkType.LINK, reference.url.unescape(), null)
            renderLink(resolvedLink.url, reference.reference.toString(), html, node, context) {
                if (node.text != null) node.text.normalizeEOL().trimEnd() else null
            }
        }
    }

    private fun render(node: Heading, context: NodeRendererContext, html: HtmlWriter) {
        html.srcPos(node.text).withAttr().tagLine("h" + node.level) {
            context.renderChildren(node)
            if (context.htmlOptions.renderHeaderId) {
                val id = context.getNodeId(node)
                if (id != null) {
                    html.openTag("ac:structured-macro", mapOf("ac:name" to "anchor"))
                    html.addParameter("", id)
                    html.closeTag("ac:structured-macro")
                }
            }
        }
    }

    private fun render(node: OrderedList, context: NodeRendererContext, html: HtmlWriter) {
        if (node.taskList) {
            renderTaskList(node, context, html)
        } else {
            val start: Int = node.startNumber
            if (listOptions.isOrderedListManualStart && start != 1) html.attr("start", start.toString())
            html.withAttr().tagIndent("ol") { context.renderChildren(node) }
        }
    }

    private fun render(node: BulletList, context: NodeRendererContext, html: HtmlWriter) {
        if (node.taskList) {
            renderTaskList(node, context, html)
        } else {
            html.withAttr().tagIndent("ul") { context.renderChildren(node) }
        }
    }

    private fun renderTaskList(node: ListBlock, context: NodeRendererContext, html: HtmlWriter) {
        html.tag("ac:task-list", false, true) {
            node.children.filterIsInstance<TaskListItem>().forEach { taskItem ->
                html.line()
                html.tag("ac:task") {
                    html.tag("ac:task-status") {
                        html.text(
                            when (taskItem.isItemDoneMarker) {
                                true -> "complete"
                                false -> "incomplete"
                            }
                        )
                    }
                    html.tag("ac:task-body") {
                        context.renderChildren(taskItem)
                    }
                }
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun render(node: TocBlock, context: NodeRendererContext, html: HtmlWriter) {
        html.tagLine("p") {
            val attributes: Map<String, String> = (node.attributesMap + parseTocOptions(node.style.toString())
                    ).filterKeys { it in ALLOWED_TOC_ATTRIBUTES }
            if (attributes.isEmpty()) {
                html.voidTag("ac:structured-macro", mapOf("ac:name" to "toc"))
            } else {
                html.withAttr().attr("ac:name", "toc").tagIndent("ac:structured-macro") {
                    attributes.forEach { (name, value) -> html.addParameter(name, value, withTagLine = true) }
                }
            }
        }
    }

    private fun parseTocOptions(options: String): Map<String, String> {
        return OPTIONS_ITEM_REGEX.findAll(options).map {
            it.groups["key"]?.value!! to (it.groups["value"] ?: it.groups["quotedvalue"])!!.value
        }.toMap()
    }

    private fun render(node: AdmonitionBlock, context: NodeRendererContext, html: HtmlWriter) {
        val type = node.info.toString().lowercase()
        val confluenceType = if (type in ADMONITION_TYPES) type else DEFAULT_ADMONITION_TYPE
        html.openTag("ac:structured-macro", mapOf("ac:name" to confluenceType))
        if (!node.title.isNull) {
            html.addParameter("title", node.title.toString())
        }
        html.tag("ac:rich-text-body") {
            context.renderChildren(node)
        }
        html.closeTag("ac:structured-macro")
    }

    private fun render(
        node: ConfluenceStatusNode,
        @Suppress("UNUSED_PARAMETER") context: NodeRendererContext,
        html: HtmlWriter
    ) {
        html.macro("status") {
            addParameter("title", node.text)
            addParameter("colour",
                node.color.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
        }
    }

    private fun render(
        node: ConfluenceUserNode,
        @Suppress("UNUSED_PARAMETER") context: NodeRendererContext,
        html: HtmlWriter
    ) {
        html.tag("ac:link") {
            html.voidTag("ri:user", mapOf("ri:username" to node.text))
        }
    }

    private val ListBlock.taskList: Boolean
        get() = children.all { it is TaskListItem }

    private fun render(node: TaskListItem, context: NodeRendererContext, html: HtmlWriter) {
        html.tagLine("li") {
            node.markerSuffix?.let { html.text(it.unescape()).text(" ") }
            context.renderChildren(node)
        }
    }

    private val Node.withRichFormatting: Boolean
        get() = !children.all { it is Text }

    private fun HtmlWriter.tagWithCData(
        tagName: String,
        text: String
    ) {
        tag(tagName)
            .raw("<![CDATA[").raw(text).raw("]]>")
            .closeTag(tagName)
    }

}
