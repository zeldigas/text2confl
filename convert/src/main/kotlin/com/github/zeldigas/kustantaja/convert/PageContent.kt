package com.github.zeldigas.kustantaja.convert

import java.nio.file.Path
import java.security.MessageDigest

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
    val name: String, val location: Path
) {
    val safeName: String
        get() = name.replace("../", "__").replace("./", "")
            .replace("/", "_")
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
        digest.fold(StringBuilder()) { builder, it -> builder.append("%02x".format(it)) }.toString()
    }
}
