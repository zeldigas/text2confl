package com.github.zeldigas.text2confl.convert

import com.github.zeldigas.text2confl.convert.confluence.LanguageMapper
import com.github.zeldigas.text2confl.convert.confluence.ReferenceProvider
import java.nio.file.Path

interface FileConverter {

    fun readHeader(file: Path, context: HeaderReadingContext): PageHeader

    fun convert(file: Path, context: ConvertingContext): PageContent

}

class ConversionFailedException(val file: Path, message: String, cause: Exception? = null) :
    RuntimeException(message, cause)

data class HeaderReadingContext(
    val titleTransformer: (Path, String) -> String
)

data class ConvertingContext(
    val referenceProvider: ReferenceProvider,
    val conversionParameters: ConversionParameters,
    val targetSpace: String
) {
    val languageMapper: LanguageMapper
        get() = conversionParameters.languageMapper

    val titleTransformer: (Path, String) -> String
        get() = conversionParameters.titleConverter
    
    fun autotextFor(file: Path): String {
        val pathFromRoot = referenceProvider.pathFromDocsRoot(file)
        return conversionParameters.noteText
            .replace("__doc-root__", conversionParameters.docRootLocation)
            .replace("__file__", pathFromRoot.toString())
    }
}


class AttachmentsRegistry {
    private val attachments: MutableMap<String, Attachment> = hashMapOf()

    fun register(ref: String, attachment: Attachment, overwrite: Boolean = false) {
        attachments[ref] = attachment
    }

    fun hasRef(ref: String) = ref in attachments

    fun ref(ref: String): Attachment = attachments.getValue(ref)

    val collectedAttachments: Map<String, Attachment>
        get() = attachments
}