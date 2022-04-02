package com.github.zeldigas.text2confl.convert.markdown.ext

import com.vladsch.flexmark.ext.attributes.AttributesExtension
import com.vladsch.flexmark.ext.attributes.FencedCodeAddType
import com.vladsch.flexmark.ext.attributes.internal.AttributesInlineParserExtension
import com.vladsch.flexmark.ext.attributes.internal.AttributesNodePostProcessor
import com.vladsch.flexmark.ext.attributes.internal.AttributesNodeRenderer
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataHolder

/**
 * Simplified version of [AttributesExtension] that does not register [com.vladsch.flexmark.ext.attributes.internal.AttributesAttributeProvider]
 * for html renderer.
 */
internal class SimpleAttributesExtension : Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {

    override fun parserOptions(options: MutableDataHolder) {
        if (options.contains(AttributesExtension.FENCED_CODE_INFO_ATTRIBUTES) && AttributesExtension.FENCED_CODE_INFO_ATTRIBUTES[options] && !options.contains(
                AttributesExtension.FENCED_CODE_ADD_ATTRIBUTES
            )
        ) {
            // change default to pre only, to add to code use attributes after info
            options.set(AttributesExtension.FENCED_CODE_ADD_ATTRIBUTES, FencedCodeAddType.ADD_TO_PRE)
        }
    }

    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.postProcessorFactory(AttributesNodePostProcessor.Factory())
        parserBuilder.customInlineParserExtensionFactory(AttributesInlineParserExtension.Factory())
    }

    override fun extend(htmlRendererBuilder: HtmlRenderer.Builder, rendererType: String) {
        if (AttributesExtension.ASSIGN_TEXT_ATTRIBUTES[htmlRendererBuilder]) {
            htmlRendererBuilder.nodeRendererFactory(AttributesNodeRenderer.Factory())
        }
    }

    override fun rendererOptions(options: MutableDataHolder) {}

}