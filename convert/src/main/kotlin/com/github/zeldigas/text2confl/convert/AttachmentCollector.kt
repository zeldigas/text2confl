package com.github.zeldigas.text2confl.convert

import com.github.zeldigas.text2confl.convert.confluence.ReferenceProvider
import mu.KotlinLogging
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

class AttachmentCollector(
    private val referencesProvider: ReferenceProvider,
    private val attachmentsRegistry: AttachmentsRegistry
) {

    companion object {
        private val logger = KotlinLogging.logger { }
    }


    fun collect(source: Path, pathToFile: String, referenceName: String? = null): Attachment? {
        if (!isLocal(pathToFile)) return null

        val effectiveName = referenceName ?: pathToFile

        if (attachmentsRegistry.hasRef(effectiveName)) return attachmentsRegistry.ref(effectiveName);
        if (referencesProvider.resolveReference(source, pathToFile) != null) return null

        val parentDir = source.parent ?: Paths.get(".")

        val file = parentDir.resolve(pathToFile).normalize()
        return if (file.exists()) {
            logger.debug { "File exists, adding as attachment: $file with ref $effectiveName" }
            val attachment = Attachment.fromLink(effectiveName, file)
            attachmentsRegistry.register(effectiveName, attachment)
            attachment
        } else {
            logger.warn { "Unresolved local ref in [${source}], ref=${referenceName ?: "(inline)"}. File does not exist: $file" }
            null
        }
    }

    private fun isLocal(pathToFile: String): Boolean {
        if (pathToFile.isEmpty() || pathToFile.startsWith("#")) return false

        return try {
            val uri = URI.create(pathToFile)
            uri.scheme == null
        } catch (e: Exception) {
            logger.trace(e) { "Failed to create uri from $pathToFile, considering as non-local" }
            false
        }
    }


}