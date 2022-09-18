package com.github.zeldigas.text2confl.convert

import org.xml.sax.ErrorHandler
import org.xml.sax.SAXParseException
import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.stream.Location
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent
import kotlin.io.path.inputStream


data class Page(
    val content: PageContent,
    val source: Path,
    val children: List<Page>
) {
    val title: String
        get() = content.header.title
}

data class PageHeader(
    val title: String, val attributes: Map<String, Any?>
)

data class Attachment(
    val attachmentName: String, val linkName: String, val resourceLocation: Path
) {

    companion object {
        fun fromLink(name: String, location: Path): Attachment {
            return Attachment(normalizeName(name), name, location)
        }

        private fun normalizeName(name: String): String {
            return name.replace("../", "__").replace("./", "")
                .replace("/", "_")
        }
    }

    val hash: String by lazy {
        val md = MessageDigest.getInstance("SHA-256")
        resourceLocation.inputStream().use {
            val byteArray = ByteArray(4096)
            val digestInputStream = DigestInputStream(it, md)
            while (digestInputStream.read(byteArray) != -1) {
            }
        }
        toBase64(md.digest())
    }
}

private val XML_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();

private object NullErrorHandler : ErrorHandler {
    override fun warning(p0: SAXParseException?) = Unit

    override fun error(p0: SAXParseException?) = Unit

    override fun fatalError(p0: SAXParseException?) = Unit
}

data class PageContent(
    val header: PageHeader,
    val body: String,
    val attachments: List<Attachment>
) {
    val hash by lazy {
        val bytes = body.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        toBase64(digest)
    }

    fun validate(): Validation {
        val stack: Deque<StartElement> = LinkedList()

        try {
            for (event in traverseDocument(body)) {
                when {
                    event.isStartElement -> stack.push(event.asStartElement())
                    event.isEndElement -> stack.pop()
                }
            }
        } catch (e: XMLStreamException) {
            val message = (e.message ?: "Unknown error occured").substringAfter("Message: ")
            return if (message.contains("must be terminated by the matching")) {
                val startTag = stack.pop()
                Validation.Invalid("${e.location.formatted()} $message Start tag location - ${startTag.location.formatted()}")
            } else {
                Validation.Invalid("${e.location.formatted()} $message")
            }

        }
        return Validation.Ok
    }
}

sealed class Validation {
    object Ok : Validation()
    data class Invalid(val issue: String) : Validation()
}

fun traverseDocument(body: String) = sequence<XMLEvent> {
    val inputFactory = XMLInputFactory.newInstance()
    inputFactory.setProperty("javax.xml.stream.isNamespaceAware", false);
    val eventReader: XMLEventReader =
        inputFactory.createXMLEventReader(
            ByteArrayInputStream("<r>\n${body}\n</r>".toByteArray()),
            "utf-8"
        )
    while (eventReader.hasNext()) {
        yield(eventReader.nextEvent())
    }
}

private fun Location.formatted() = "[${lineNumber - 1}:${columnNumber}]"

private fun toBase64(digest: ByteArray) =
    digest.fold(StringBuilder()) { builder, it -> builder.append("%02x".format(it)) }.toString()
