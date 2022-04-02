package com.github.zeldigas.text2confl.convert.markdown

import com.github.zeldigas.text2confl.convert.*
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterBlock
import com.vladsch.flexmark.util.ast.Document
import java.nio.file.Files
import java.nio.file.Path


internal class MarkdownFileConverter(private val parser: MarkdownParser) : FileConverter {

    constructor(config: MarkdownConfiguration): this(MarkdownParser(config))

    override fun readHeader(file: Path, context: HeaderReadingContext): PageHeader {
        val (header, _) = parseToHeaderAndBody(file, context.titleTransformer)
        return header
    }

    override fun convert(file: Path, context: ConvertingContext): PageContent {
        val (header, ast) = parseToHeaderAndBody(file, context.titleTransformer)

        val attachments = collectAttachments(file, context, ast)
        val generator = parser.htmlRenderer(file, attachments, context)
        return PageContent(
            header,
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

    private fun parseToHeaderAndBody(file: Path, titleTransformer: (Path, String) -> String): Pair<PageHeader, Document> {
        val ast = parseToAst(file)

        val attributes = readAttributes(ast)
        return createHeader(attributes, file, ast, titleTransformer)
    }

    private fun createHeader(
        attributes: Map<String, Any>,
        file: Path,
        document: Document,
        titleTransformer: (Path, String) -> String
    ): Pair<PageHeader, Document> {
        val title = attributes["title"]?.toString()
            ?: documentTitle(document)
            ?: file.toFile().nameWithoutExtension
        return PageHeader(titleTransformer(file, title), attributes) to document
    }

    private fun documentTitle(document: Document): String? {
        val firstChild = document.children.firstOrNull { it !is YamlFrontMatterBlock }
        if (firstChild is Heading && firstChild.level == 1) {
            val title = firstChild.text.unescape()
            firstChild.unlink()
            return title
        }
        return null
    }

    private fun readAttributes(ast: Document): Map<String, Any> =
        AbstractYamlFrontMatterVisitor().let {
            it.visit(ast)
            it.data.mapValues { (_, v) -> if (v.size == 1) v.first() else v }
        }
}