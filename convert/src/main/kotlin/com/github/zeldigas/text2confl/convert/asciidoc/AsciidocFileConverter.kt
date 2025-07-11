package com.github.zeldigas.text2confl.convert.asciidoc

import com.github.zeldigas.text2confl.convert.*
import com.github.zeldigas.text2confl.convert.confluence.ReferenceProvider
import com.vladsch.flexmark.util.sequence.Escaping
import org.asciidoctor.ast.Document
import java.nio.file.Path

class AsciidocFileConverter(private val asciidocParser: AsciidocParser, private val workdirRoot: Path) : FileConverter {

    constructor(config: AsciidoctorConfiguration) : this(AsciidocParser(config), config.workdir)

    override fun readHeader(file: Path, context: HeaderReadingContext): PageHeader {
        val document = asciidocParser.parseDocumentHeader(file)

        return toHeader(file, document, context.titleTransformer)
    }

    private fun toHeader(file: Path, document: Document, titleTransformer: (Path, String) -> String): PageHeader {
        val attributes = document.attributes.mapValues { (_, v) -> parseAttribute(v) }

        return PageHeader(
            titleTransformer(file, computeTitle(document, file, attributes)),
            attributes,
            listOf("keywords", "labels")
        )
    }

    private fun computeTitle(doc: Document, file: Path, attributes: Map<String, Any>): String =
        attributes["title"]?.toString()
            ?: documentTitle(doc, attributes)
            ?: file.toFile().nameWithoutExtension

    private fun documentTitle(doc: Document, attributes: Map<String, Any>): String? {
        val title = doc.doctitle ?: return null
        return Escaping.unescapeHtml(attributes.entries.fold(title) { current, (key, value) ->
            current.replace(
                "{$key}",
                value.toString()
            )
        })
    }

    override fun convert(file: Path, context: ConvertingContext): PageContent {
        val workdir = fileWorkdir(file, context.referenceProvider)
        val attachmentsRegistry = AttachmentsRegistry()
        val document = try {
            asciidocParser.parseDocument(file, createAttributes(file, context, attachmentsRegistry, workdir))
        } catch (ex: Exception) {
            throw ConversionFailedException(file, "Document parsing failed", ex)
        }

        val body = try {
            document.setAttribute("t2c-auto-text", context.autotextFor(file), true)
            document.setAttribute("t2c-add-auto-text", context.conversionParameters.addAutogeneratedNote, true)
            document.convert()
        } catch (ex: Exception) {
            throw ConversionFailedException(file, "Document conversion failed", ex)
        }

        return PageContent(
            toHeader(file, document, context.titleTransformer),
            body,
            attachmentsRegistry.collectedAttachments.values.toList()
        )
    }

    private fun fileWorkdir(file: Path, referenceProvider: ReferenceProvider): Path {
        return workdirRoot.resolve(referenceProvider.pathFromDocsRoot(file))
    }

    private fun createAttributes(file: Path, context: ConvertingContext, registry: AttachmentsRegistry, workdir: Path) =
        AsciidocRenderingParameters(
            context.languageMapper,
            AsciidocReferenceProvider(file, context.referenceProvider),
            context.autotextFor(file),
            context.conversionParameters.addAutogeneratedNote,
            context.targetSpace,
            AsciidocAttachmentCollector(file, AttachmentCollector(context.referenceProvider, registry), workdir),
            extraAttrs = mapOf(
                "outdir" to workdir.toString(),
                "imagesoutdir" to workdir.toString(),
                "t2c-editor-version" to context.conversionParameters.editorVersion.name.lowercase()
            )
        )
}