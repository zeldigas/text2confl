package com.github.zeldigas.text2confl.convert.markdown

import com.vladsch.flexmark.html.HtmlWriter

fun HtmlWriter.macro(name: String, block: HtmlWriter.() -> Unit) {
    openTag("ac:structured-macro", mapOf("ac:name" to name))
    this.block()
    closeTag("ac:structured-macro")
}

fun HtmlWriter.addParameter(name: String, value: String, withTagLine: Boolean = false) {
    attr("ac:name", name).withAttr().tag("ac:parameter")
    text(value).closeTag("ac:parameter")
    if (withTagLine) {
        line()
    }
}

fun HtmlWriter.openTag(name: String, attrs: Map<String, CharSequence> = emptyMap()): HtmlWriter {
    addAttributes(attrs)
    return tag(name)
}

fun HtmlWriter.voidTag(name: String, attrs: Map<String, CharSequence> = emptyMap()): HtmlWriter {
    addAttributes(attrs)
    return tagVoid(name)
}

fun HtmlWriter.addAttributes(attrs: Map<String, CharSequence>) {
    if (attrs.isNotEmpty()) {
        attrs.forEach { (k, v) -> attr(k, v) }
        withAttr()
    }
}