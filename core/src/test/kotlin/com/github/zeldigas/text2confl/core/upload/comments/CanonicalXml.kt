package com.github.zeldigas.text2confl.core.upload.comments

import org.jsoup.nodes.Entities
import java.io.StringReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants

/**
 * Canonical form of a storage format fragment for comparison in tests. Uses StAX parser (independent of jsoup), so it
 * also verifies that jsoup serialization is valid XML.
 *
 * Attributes are sorted, adjacent text is joined, entities are decoded. CDATA is kept distinct from regular text.
 */
object CanonicalXml {
    private val NAMED_ENTITY = Regex("&([A-Za-z][A-Za-z0-9]*);")
    private val XML_ENTITIES = setOf("amp", "lt", "gt", "quot", "apos")

    fun canonical(fragment: String): String {
        val factory = XMLInputFactory.newInstance().apply {
            setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false)
            setProperty(XMLInputFactory.IS_COALESCING, false)
            setProperty(XMLInputFactory.SUPPORT_DTD, false)
            runCatching { setProperty("http://java.sun.com/xml/stream/properties/report-cdata-event", true) }
        }
        val reader = factory.createXMLStreamReader(StringReader("<root>${replaceNamedEntities(fragment)}</root>"))
        val out = StringBuilder()
        var depth = -1
        var pendingText: StringBuilder? = null
        var pendingCdata: StringBuilder? = null

        fun indent() = "  ".repeat(maxOf(depth, 0))
        fun flushText() {
            pendingText?.let { out.append(indent()).append("TEXT ").append(escape(it)).append('\n') }
            pendingCdata?.let { out.append(indent()).append("CDATA ").append(escape(it)).append('\n') }
            pendingText = null
            pendingCdata = null
        }

        while (reader.hasNext()) {
            when (reader.next()) {
                XMLStreamConstants.START_ELEMENT -> {
                    flushText()
                    depth++
                    if (depth > 0) {
                        val attrs = (0 until reader.attributeCount)
                            .map { reader.getAttributeName(it).let { n -> if (n.prefix.isNullOrEmpty()) n.localPart else "${n.prefix}:${n.localPart}" } to reader.getAttributeValue(it) }
                            .sortedBy { it.first }
                            .joinToString("") { (k, v) -> " $k=\"${escape(v)}\"" }
                        val name = reader.name.let { if (it.prefix.isNullOrEmpty()) it.localPart else "${it.prefix}:${it.localPart}" }
                        out.append("  ".repeat(depth - 1)).append('<').append(name).append(attrs).append(">\n")
                    }
                }

                XMLStreamConstants.END_ELEMENT -> {
                    flushText()
                    depth--
                }

                XMLStreamConstants.CHARACTERS, XMLStreamConstants.SPACE -> {
                    if (pendingCdata != null) flushText()
                    (pendingText ?: StringBuilder().also { pendingText = it }).append(reader.text)
                }

                XMLStreamConstants.CDATA -> {
                    if (pendingText != null) flushText()
                    (pendingCdata ?: StringBuilder().also { pendingCdata = it }).append(reader.text)
                }
            }
        }
        return out.toString()
    }

    private fun escape(text: CharSequence) = text.toString()
        .replace("\n", "\\n")
        .replace(" ", "\\u00A0")

    private fun replaceNamedEntities(text: String) = NAMED_ENTITY.replace(text) { match ->
        val name = match.groupValues[1]
        if (name in XML_ENTITIES) {
            match.value
        } else {
            val decoded = Entities.getByName(name)
            if (decoded.isEmpty()) match.value else decoded.codePoints().toArray().joinToString("") { "&#$it;" }
        }
    }
}
