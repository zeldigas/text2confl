package com.github.zeldigas.text2confl.convert.markdown

import com.github.zeldigas.text2confl.convert.*
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.util.ast.Document
import java.nio.file.Files
import java.nio.file.Path


internal class MarkdownFileConverter(private val parser:MarkdownParser = MarkdownParser()) : FileConverter {

    override fun readHeader(file: Path, context: HeaderReadingContext): PageHeader {
        val ast = parseToAst(file)

        val attributes = readAttributes(ast)
        return createHeader(attributes, file, context.titleTransformer)
    }

    override fun convert(file: Path, context: ConvertingContext): PageContent {
        val ast = parseToAst(file)

        val attributes = readAttributes(ast)
        val attachments = collectAttachments(file, context, ast)
        val generator = parser.htmlRenderer(file, attachments, context)
        return PageContent(
            createHeader(attributes, file, context.titleTransformer),
            generator.render(ast),
            attachments.values.toList()
        )
    }

    private fun parseToAst(file: Path): Document {
        return try {
            Files.newBufferedReader(file, Charsets.UTF_8).use { parser.parseReader(it) }
        }catch (ex: Exception) {
            throw ConversionFailedException(file, "Document parsing failed", ex)
        }
    }

    private fun collectAttachments(
        file: Path,
        context: ConvertingContext,
        ast: Document
    ): Map<String, Attachment> {
        try {
            return AttachmentCollector(file, context.referenceProvider).collectAttachments(ast)
                .map { (name, path) -> name to Attachment.fromLink(name, path) }.toMap()
        } catch (ex: Exception) {
            throw ConversionFailedException(file, "Failed to extract attachments", ex)
        }
    }

    private fun createHeader(
        attributes: Map<String, Any>,
        file: Path,
        titleTransformer: (Path, String) -> String
    ): PageHeader {
        val title = attributes["title"]?.toString() ?: file.toFile().nameWithoutExtension
        return PageHeader(titleTransformer(file, title), attributes)
    }

    private fun readAttributes(ast: Document): Map<String, Any> =
        AbstractYamlFrontMatterVisitor().let {
            it.visit(ast)
            it.data.mapValues { (_, v) -> if (v.size == 1) v.first() else v }
        }
}