package com.github.zeldigas.kustantaja.convert.markdown

import com.github.zeldigas.kustantaja.convert.ConvertingContext
import com.vladsch.flexmark.ast.FencedCodeBlock
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.HtmlRenderer.HtmlRendererExtension
import com.vladsch.flexmark.html.HtmlRendererOptions
import com.vladsch.flexmark.html.HtmlWriter
import com.vladsch.flexmark.html.renderer.CoreNodeRenderer
import com.vladsch.flexmark.html.renderer.NodeRenderer
import com.vladsch.flexmark.html.renderer.NodeRendererContext
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.DataHolder
import com.vladsch.flexmark.util.data.DataKey
import com.vladsch.flexmark.util.data.MutableDataHolder
import com.vladsch.flexmark.util.data.NullableDataKey
import com.vladsch.flexmark.util.sequence.BasedSequence
import java.nio.file.Path

internal class ConfluenceFormatExtension() : HtmlRendererExtension {

    companion object {
        val ATTACHMENTS = DataKey<Map<String, Path>>("FENCED_CODE_CONTENT_BLOCK", emptyMap())
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
    private val attachments: Map<String, Path> = ConfluenceFormatExtension.ATTACHMENTS[options]
    private val convertingContext: ConvertingContext = ConfluenceFormatExtension.CONTEXT[options]!!

    companion object {
        val LANG_REMAPPING = mapOf(
            "yaml" to "yml",
            "shell" to "bash",
            "zsh" to "bash",
            "sh" to "bash",
            "html" to "xml",
        )
        val SUPPORTED_LANGS = setOf(
            "abap", "actionscript3", "ada",
            "applescript", "arduino", "autoit",
            "bash", "c", "cpp", "clojure",
            "coffeescript", "coldfusion", "c#",
            "css", "cude", "d",
            "dart", "diff", "elixir",
            "erl", "fortran", "foxpro",
            "go", "graphql", "groovy",
            "haskell", "haxe", "html",
            "java", "javafx", "js",
            "json", "jsx", "julia",
            "kotlin", "livescript", "lua",
            "mathematica", "matlab", "objective-c",
            "objective-j", "ocaml", "octave",
            "pas", "perl", "php",
            "text", "powershell", "prolog",
            "puppet", "py", "qbs",
            "r", "racket", "restructuredtext",
            "ruby", "rust", "sass",
            "scala", "scheme", "smalltalk",
            "splunk-spl", "sql", "standardlm",
            "swift", "tcl", "tex",
            "tsx", "typescript", "vala",
            "vbnet", "verilog", "vhdl",
            "vb", "xml", "xquery", "yaml"
        )
    }

    override fun getNodeRenderingHandlers(): Set<NodeRenderingHandler<*>> {
        return setOf(
            NodeRenderingHandler(FencedCodeBlock::class.java, this::render)
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
                    val remappedLang = LANG_REMAPPING.getOrDefault(lang, lang)
                    if (remappedLang in SUPPORTED_LANGS) {
                        lang
                    } else {
                        null
                    }
                }
            if (language != null) {
                html.addParameter("language", language)
            }
            html.tag("ac:plain-text-body")
                .raw("<![CDATA[").raw(node.contentChars.normalizeEOL().trimEnd()).raw("]]>")
                .closeTag("ac:plain-text-body")
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

    private fun HtmlWriter.addParameter(name: String, value: String) {
        attr("ac:name", name).withAttr().tag("ac:parameter").text(value).closeTag("ac:parameter")
    }
}
