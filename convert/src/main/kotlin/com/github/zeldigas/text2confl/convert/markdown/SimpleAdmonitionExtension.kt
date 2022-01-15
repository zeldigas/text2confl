package com.github.zeldigas.text2confl.convert.markdown

import com.vladsch.flexmark.ext.admonition.internal.AdmonitionBlockParser
import com.vladsch.flexmark.ext.attributes.AttributesExtension
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataHolder

/**
 * Simplified version of [AttributesExtension] that does not register [com.vladsch.flexmark.ext.admonition.internal.AdmonitionNodeRenderer]
 * for html renderer.
 */
internal class SimpleAdmonitionExtension : Parser.ParserExtension {

    override fun parserOptions(options: MutableDataHolder) {
    }

    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.customBlockParserFactory(AdmonitionBlockParser.Factory())
    }

}