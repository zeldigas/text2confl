package com.github.zeldigas.kustantaja.convert.markdown

import com.github.zeldigas.kustantaja.convert.Attachment
import com.github.zeldigas.kustantaja.convert.ConvertingContext
import com.vladsch.flexmark.ast.FencedCodeBlock
import com.vladsch.flexmark.ast.Image
import com.vladsch.flexmark.ast.Link
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
import com.vladsch.flexmark.util.sequence.BasedSequence
import com.vladsch.flexmark.util.sequence.Escaping

internal class ConfluenceFormatExtension() : HtmlRendererExtension {

    companion object {
        val ATTACHMENTS = DataKey<Map<String, Attachment>>("FENCED_CODE_CONTENT_BLOCK", emptyMap())
        val CONTEXT = NullableDataKey<ConvertingContext>("FENCED_CODE_CONTENT_BLOCK", null)
    }

    override fun rendererOptions(options: MutableDataHolder) {

    }

    override fun extend(htmlRendererBuilder: HtmlRenderer.Builder, rendererType: String) {
        htmlRendererBuilder.nodeRendererFactory { ConfluenceNodeRenderer(it) }
    }
}

class ConfluenceNodeRenderer(options: DataHolder) : NodeRenderer {

    private val codeContentBlock = Parser.FENCED_CODE_CONTENT_BLOCK[options]
    private val attachments: Map<String, Attachment> = ConfluenceFormatExtension.ATTACHMENTS[options]
    private val convertingContext: ConvertingContext = ConfluenceFormatExtension.CONTEXT[options]!!
    private val basicRenderer = CoreNodeRenderer(options)

    override fun getNodeRenderingHandlers(): Set<NodeRenderingHandler<*>> {
        return setOf(
            NodeRenderingHandler(FencedCodeBlock::class.java, this::render),
            NodeRenderingHandler(Image::class.java, this::render),
            NodeRenderingHandler(Link::class.java, this::render),
        )
    }

    private fun render(node: FencedCodeBlock, context: NodeRendererContext, html: HtmlWriter) {
        html.line()

        val info: BasedSequence = node.info
        val htmlOptions: HtmlRendererOptions = context.htmlOptions
        val hasLanguageTag = info.isNotNull && !info.isBlank

        if (hasLanguageTag) {
            html.attr("ac:name", "code").withAttr().tag("ac:structured-macro").openPre()
            val language: String? =
                node.getInfoDelimitedByAny(htmlOptions.languageDelimiterSet).unescape().let { lang ->
                    convertingContext.languageMapper.mapToConfluenceLanguage(lang)
                }
            if (language != null) {
                html.addParameter("language", language)
            }
            plainTextBodyWithCdata(html, node.contentChars.normalizeEOL().trimEnd())
            html.closeTag("ac:structured-macro")
        } else {
            html.srcPosWithTrailingEOL(node.chars).withAttr().tag("pre").openPre()
            val noLanguageClass = htmlOptions.noLanguageClass.trim()
            if (!noLanguageClass.isEmpty()) {
                html.attr("class", noLanguageClass)
            }
            html.srcPosWithEOL(node.contentChars).withAttr(CoreNodeRenderer.CODE_CONTENT).tag("code")
            if (codeContentBlock) {
                context.renderChildren(node)
            } else {
                html.text(node.contentChars.normalizeEOL())
            }
            html.tag("/code")
            html.tag("/pre")
        }
        html.closePre()

        html.lineIf(htmlOptions.htmlBlockCloseTagEol)
    }

    private fun render(node: Image, context: NodeRendererContext, html: HtmlWriter) {
        val altText = TextCollectingVisitor().collectAndGetText(node)
        val url = node.url.unescape()
        html.attr("ac:alt", altText)
            .attr("ac:title", altText)
            .withAttr()
            //todo support custom attributes via attributes extension: alignment, width, height, caption
            .tag("ac:image")
        if (url in attachments) {
            html.attr("ri:filename", attachments.getValue(url).attachmentName)
                .withAttr()
                .tagVoid("ri:attachment")
        } else {
            buildUrl(context, node)
            html.attr("ri:value", url)
                .withAttr()
                .tagVoid("ri:url")
        }
        html.closeTag("ac:image")
    }

    private fun buildUrl(
        context: NodeRendererContext,
        node: Image
    ) {
        //code taken from standard renderer
        val resolvedLink = context.resolveLink(LinkType.IMAGE, node.url.unescape(), null, null)
        var linkUrl: String? = resolvedLink.url

        if (!node.urlContent.isEmpty) {
            // reverse URL encoding of =, &
            val content = Escaping.percentEncodeUrl(node.urlContent).replace("+", "%2B").replace("%3D", "=")
                .replace("%26", "&amp;")
            linkUrl += content
        }
    }

    private fun render(node: Link, context: NodeRendererContext, html: HtmlWriter) {
        val url = node.url.unescape()
//        todo support for xrefs
        if (url in attachments) {
            html.tag("ac:link")
            html.attr("ri:filename", attachments.getValue(url).attachmentName)
                .withAttr()
                .tagVoid("ri:attachment")
            if (node.text != null){
                plainTextBodyWithCdata(html, node.text.normalizeEOL().trimEnd())
            }
            html.closeTag("ac:link")
        } else {
            delegateToStandardRenderer(node, context, html)
        }
    }

    private fun HtmlWriter.addParameter(name: String, value: String) {
        attr("ac:name", name).withAttr().tag("ac:parameter").text(value).closeTag("ac:parameter")
    }

    private fun plainTextBodyWithCdata(
        html: HtmlWriter,
        text: String
    ) {
        html.tag("ac:plain-text-body")
            .raw("<![CDATA[").raw(text).raw("]]>")
            .closeTag("ac:plain-text-body")
    }

    private inline fun <reified T: Node> delegateToStandardRenderer(
        node: T,
        context: NodeRendererContext,
        html: HtmlWriter
    ) {
        basicRenderer.nodeRenderingHandlers?.find { it.nodeType == T::class.java }?.render(node, context, html)
    }
}
