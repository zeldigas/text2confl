package com.github.zeldigas.text2confl.convert.markdown

import assertk.Assert
import assertk.assertions.isEqualTo
import com.github.zeldigas.text2confl.convert.*
import com.github.zeldigas.text2confl.convert.confluence.LanguageMapper
import com.github.zeldigas.text2confl.convert.confluence.LanguageMapperImpl
import com.github.zeldigas.text2confl.convert.confluence.ReferenceProvider
import com.github.zeldigas.text2confl.convert.markdown.diagram.DiagramMakers
import kotlin.io.path.Path

internal open class RenderingTestBase {

    val languageMapper: LanguageMapper = LanguageMapperImpl(setOf("java", "kotlin"), "fallback")
    val attachmentsRegistry = AttachmentsRegistry()

    fun <T> Assert<T>.isEqualToConfluenceFormat(expected: String) {
        isEqualTo(expected + "\n")
    }

    fun toHtml(
        src: String,
        attachments: Map<String, Attachment> = emptyMap(),
        referenceProvider: ReferenceProvider = ReferenceProvider.singleFile(),
        languageMapper: LanguageMapper? = null,
        addAutogenHeader: Boolean = false,
        autogenText: String = "Generated for __doc-root____file__",
        config: MarkdownConfiguration = MarkdownConfiguration(true, emptyList()),
        diagramMakers: DiagramMakers = DiagramMakers.NOP
    ): String {
        val context = ConvertingContext(
            referenceProvider,
            ConversionParameters(
                languageMapper ?: this.languageMapper,
                { _, title -> title },
                addAutogenHeader,
                noteText = autogenText,
                docRootLocation = "http://example.com/",
                markdownConfiguration = config,
                editorVersion = EditorVersion.V1
            ),
            "TEST",
        )

        val parser = MarkdownParser(config, diagramMakers)
        val ast = parser.parseString(src, context, attachmentsRegistry, Path("src.md"))

        val htmlRenderer = parser.htmlRenderer(
            Path("src.md"), attachmentsRegistry.collectedAttachments + attachments, context
        )

        return htmlRenderer.render(ast)
    }

}