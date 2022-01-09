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
    val attachmentName: String, val linkName:String, val resourceLocation: Path
) {

    companion object {
        fun fromLink(name:String, location:Path) : Attachment {
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
