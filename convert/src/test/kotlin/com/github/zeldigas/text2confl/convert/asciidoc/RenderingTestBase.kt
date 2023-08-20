package com.github.zeldigas.text2confl.convert.asciidoc

import assertk.Assert
import assertk.assertions.isEqualTo
import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.AttachmentCollector
import com.github.zeldigas.text2confl.convert.AttachmentsRegistry
import com.github.zeldigas.text2confl.convert.confluence.LanguageMapper
import com.github.zeldigas.text2confl.convert.confluence.LanguageMapperImpl
import com.github.zeldigas.text2confl.convert.confluence.ReferenceProvider
import kotlin.io.path.Path

internal open class RenderingTestBase {

    companion object {
        val DEFAULT_PARSER = AsciidocParser(
            AsciidoctorConfiguration(
                libsToLoad = listOf("asciidoctor-diagram"), loadBundledMacros = true
            )
        )
    }

    val languageMapper: LanguageMapper = LanguageMapperImpl(setOf("java", "kotlin"), "fallback")

    fun <T> Assert<T>.isEqualToConfluenceFormat(expected: String) {
        isEqualTo(expected)
    }

    fun toHtml(
        src: String,
        attachments: Map<String, Attachment> = emptyMap(),
        languageMapper: LanguageMapper? = null,
        addAutogenHeader: Boolean = false,
        autogenText: String = "Generated for __doc-root____file__",
        parser: AsciidocParser = DEFAULT_PARSER,
        attachmentsCollector: AsciidocAttachmentCollector? = null,
        referenceProvider: AsciidocReferenceProvider? = null,
    ): String {
        val defaultSource = Path("./test.adoc")
        val effectiveReferenceProvider =
            referenceProvider ?: AsciidocReferenceProvider(defaultSource, ReferenceProvider.singleFile())
        val effectiveCollector = attachmentsCollector
            ?: AsciidocAttachmentCollector(
                defaultSource,
                AttachmentCollector(effectiveReferenceProvider.referenceProvider, AttachmentsRegistry()),
                Path(".")
            )
        attachments.forEach { (name, attachment) ->
            effectiveCollector.attachmentCollector.attachmentsRegistry.register(
                name,
                attachment
            )
        }
        val ast = parser.parseDocument(
            src, AsciidocRenderingParameters(
                languageMapper ?: this.languageMapper,
                effectiveReferenceProvider,
                autogenText,
                addAutogenHeader,
                "TEST",
                effectiveCollector
            )
        )

        return ast.convert()
    }

}