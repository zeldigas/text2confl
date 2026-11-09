package com.github.zeldigas.text2confl.core.upload.comments

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities
import org.jsoup.parser.Parser

/**
 * Parsing and serialization of Confluence storage format fragments.
 *
 * Fragment is wrapped into synthetic root element, so that it can contain several top-level nodes.
 */
internal object StorageXml {
    private const val ROOT = "t2c-root"

    fun parse(body: String): Element {
        val document = Jsoup.parse("<$ROOT>$body</$ROOT>", "", Parser.xmlParser())
        document.outputSettings()
            .prettyPrint(false)
            .syntax(Document.OutputSettings.Syntax.xml)
            .escapeMode(Entities.EscapeMode.xhtml)
            .charset(Charsets.UTF_8)
        return document.child(0)
    }

    fun serialize(root: Element): String = root.html()
}
