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
}


class AttachmentsRegistry {
    private val attachments: MutableMap<String, Attachment> = hashMapOf()

    fun register(ref: String, attachment: Attachment, overwrite: Boolean = false) {
        attachments[ref] = attachment
    }

    fun hasRef(ref: String) = ref in attachments

    val collectedAttachments: Map<String, Attachment>
        get() = attachments
}