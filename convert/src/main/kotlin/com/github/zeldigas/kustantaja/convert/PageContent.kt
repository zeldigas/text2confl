package com.github.zeldigas.kustantaja.convert

import java.nio.file.Path
import java.security.DigestInputStream
import java.security.MessageDigest
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
    val name: String, val location: Path
) {
    val hash: String by lazy {
        val md = MessageDigest.getInstance("SHA-256")
        location.inputStream().use {
            val byteArray = ByteArray(4096)
            while (DigestInputStream(it, md).read(byteArray) != -1) {
            }
        }
        toBase64(md.digest())
    }

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
        toBase64(digest)
    }
}

private fun toBase64(digest: ByteArray) =
    digest.fold(StringBuilder()) { builder, it -> builder.append("%02x".format(it)) }.toString()
