package com.github.zeldigas.text2confl.convert.asciidoc

import com.github.zeldigas.text2confl.convert.AttachmentCollector
import java.nio.file.Path
import kotlin.io.path.exists

class AsciidocAttachmentCollector(
    val source: Path,
    val attachmentCollector: AttachmentCollector,
    val workdir: Path
) {

    fun collect(target: String): String? {
        val fileInWorkdir = workdir.resolve(target)
        val attachment = if (fileInWorkdir.exists()) {
            attachmentCollector.collectRelativeToDir(workdir, target)
        } else {
            attachmentCollector.collectRelativeToSourceFile(source, target)
        }
        return attachment?.attachmentName
    }

}
