package com.github.zeldigas.text2confl.convert.markdown

import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.ConvertingContext
import com.github.zeldigas.text2confl.convert.confluence.Anchor
import com.github.zeldigas.text2confl.convert.confluence.Xref
import com.vladsch.flexmark.ast.*
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.HtmlRenderer.HtmlRendererExtension
import com.vladsch.flexmark.html.HtmlRendererOptions
import com.vladsch.flexmark.html.HtmlWriter
import com.vladsch.flexmark.html.renderer.*
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.ast.TextCollectingVisitor
import com.vladsch.flexmark.util.data.DataHolder
import com.vladsch.flexmark.util.data.DataKey
import com.vladsch.flexmark.util.data.MutableDataHolder
import com.vladsch.flexmark.util.data.NullableDataKey
import com.vladsch.flexmark.util.misc.CharPredicate
import com.vladsch.flexmark.util.sequence.BasedSequence
import com.vladsch.flexmark.util.sequence.Escaping
import mu.KotlinLogging
import java.nio.file.Path


internal class ConfluenceFormatExtension : HtmlRendererExtension {

    companion object {
        val DOCUMENT_LOCATION = NullableDataKey<Path>("DOCUMENT_LOCATION", null)
        val ATTACHMENTS = DataKey<Map<String, Attachment>>("FENCED_CODE_CONTENT_BLOCK", emptyMap())
        val CONTEXT = NullableDataKey<ConvertingContext>("FENCED_CODE_CONTENT_BLOCK", null)
    }

    override fun rendererOptions(options: MutableDataHolder) {
    }

    override fun extend(htmlRendererBuilder: HtmlRenderer.Builder, rendererType: String) {
        htmlRendererBuilder.nodeRendererFactory { ConfluenceNodeRenderer(it) }
    }
}

/**
 * Confluence storage format customizations that override [com.vladsch.flexmark.html.renderer.CoreNodeRenderer]
 */
class ConfluenceNodeRenderer(options: DataHolder) : NodeRenderer {

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val sourcePath = ConfluenceFormatExtension.DOCUMENT_LOCATION[options]!!
    private val referenceRepository = Parser.REFERENCES.get(options)
    private val recheckUndefinedReferences = HtmlRenderer.RECHECK_UNDEFINED_REFERENCES.get(options)
    private val attachments: Map<String, Attachment> = ConfluenceFormatExtension.ATTACHMENTS[options]
    private val convertingContext: ConvertingContext = ConfluenceFormatExtension.CONTEXT[options]!!
    private val basicRenderer = CoreNodeRenderer(options)

    override fun getNodeRenderingHandlers(): Set<NodeRenderingHandler<*>> {
        return setOf(
            NodeRenderingHandler(FencedCodeBlock::class.java, this::render),
            NodeRenderingHandler(Image::class.java, this::render),
            NodeRenderingHandler(ImageRef::class.java, this::render),
            NodeRenderingHandler(Link::class.java, this::render),
            NodeRenderingHandler(LinkRef::class.java, this::render),
            NodeRenderingHandler(Heading::class.java, this::render),
            )
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
        html.tagWithCData("ac:plain-text-body", node.contentChars.normalizeEOL().trimEnd())
        html.closeTag("ac:structured-macro")
    }

    private fun render(node: Image, context: NodeRendererContext, html: HtmlWriter) {
        renderImage(html, node.url.unescape(), imageAltText(node), imageTitle(node)) { buildUrl(context, node) }
    }

    private fun buildUrl(
        context: NodeRendererContext,
        node: Image
    ): String {
        //code taken from standard renderer
        val resolvedLink = context.resolveLink(LinkType.IMAGE, node.url.unescape(), null, null)
        var linkUrl: String = resolvedLink.url

        if (!node.urlContent.isEmpty) {
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
            val resolvedLink = context.resolveLink(LinkType.IMAGE, reference.url.unescape(), null)
            renderImage(
                html,
                resolvedLink.url,
                imageAltText(node),
                imageTitle(reference)
            ) { resolvedLink.url }
        }

    }

    private fun imageTitle(node: LinkNodeBase) =
        if (node.title.isNotNull) node.title.unescape() else null

    private fun imageAltText(node: Node) = TextCollectingVisitor().collectAndGetText(node)

    private fun renderImage(
        html: HtmlWriter,
        url: String,
        alt: String,
        title: String?,
        externalUrlProvider: () -> String
    ) {
        val attributes = buildMap {
            //todo support custom attributes via attributes extension: alignment, width, height, caption
            put("ac:alt", alt)
            if (title != null) {
                put("ac:title", title)
            }
        }
        html.openTag("ac:image", attributes)
        if (url in attachments) {
            html.voidTag("ri:attachment", mapOf("ri:filename" to attachments.getValue(url).attachmentName))
        } else {
            html.voidTag("ri:url", mapOf("ri:value" to externalUrlProvider()))
        }
        html.closeTag("ac:image")
    }

    private fun render(node: Link, context: NodeRendererContext, html: HtmlWriter) {
        val url = node.url.unescape()
        renderLink(url, html, node, context) {
            if (node.text != null) node.text.normalizeEOL().trimEnd() else null
        }
    }

    private inline fun <reified T: Node> renderLink(
        url: String,
        html: HtmlWriter,
        node: T,
        context: NodeRendererContext,
        noinline textExtractor: (T) -> String?
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
        } else if (url in attachments) {
            html.openTag("ac:link")
            html.voidTag("ri:attachment", mapOf("ri:filename" to attachments.getValue(url).attachmentName))
            appendLinkBody(node, html, context, textExtractor)
            html.closeTag("ac:link")
        } else {
            delegateToStandardRenderer(node, context, html)
        }
    }

    private fun <T: Node> appendLinkBody(node: T, html: HtmlWriter, context: NodeRendererContext, textExtractor: (T) -> String?) {
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
            delegateToStandardRenderer(node, context, html)
        } else {
            val reference = node.getReferenceNode(referenceRepository)!!
            val resolvedLink = context.resolveLink(LinkType.LINK, reference.url.unescape(), null)
            renderLink(resolvedLink.url, html, node, context) {
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

    private fun HtmlWriter.addParameter(name: String, value: String) {
        attr("ac:name", name).withAttr().tag("ac:parameter").text(value).closeTag("ac:parameter")
    }

    private fun HtmlWriter.openTag(name: String, attrs: Map<String, CharSequence> = emptyMap()): HtmlWriter {
        addAttributes(attrs)
        return tag(name)
    }

    private fun HtmlWriter.voidTag(name: String, attrs: Map<String, CharSequence> = emptyMap()): HtmlWriter {
        addAttributes(attrs)
        return tagVoid(name)
    }

    private fun HtmlWriter.addAttributes(attrs: Map<String, CharSequence>) {
        if (attrs.isNotEmpty()) {
            attrs.forEach { (k, v) -> attr(k, v) }
            withAttr()
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

    private inline fun <reified T : Node> delegateToStandardRenderer(
        node: T,
        context: NodeRendererContext,
        html: HtmlWriter
    ) {
        basicRenderer.nodeRenderingHandlers?.find { it.nodeType == T::class.java }?.render(node, context, html)
    }
}
