package com.github.zeldigas.text2confl.convert.asciidoc

import com.github.zeldigas.text2confl.convert.AttachmentCollector
import java.nio.file.Path

class AsciidocAttachmentCollector(
    val source: Path,
    val attachmentCollector: AttachmentCollector
) {

    fun collect(target: String): String? {
        return attachmentCollector.collect(source, target)?.attachmentName
    }

}
