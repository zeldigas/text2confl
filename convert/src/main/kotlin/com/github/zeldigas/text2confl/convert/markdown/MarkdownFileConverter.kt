package com.github.zeldigas.text2confl.convert.markdown

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.zeldigas.text2confl.convert.*
import com.github.zeldigas.text2confl.convert.markdown.diagram.createDiagramMakers
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterBlock
import com.vladsch.flexmark.util.ast.Document
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Path


internal class MarkdownFileConverter(private val parser: MarkdownParser) : FileConverter {

    constructor(config: MarkdownConfiguration) : this(MarkdownParser(config, createDiagramMakers(config.diagrams)))

    override fun readHeader(file: Path, context: HeaderReadingContext): PageHeader {
        return parseHeader(file, context.titleTransformer)
    }

    private fun parseHeader(file: Path, titleTransformer: (Path, String) -> String): PageHeader {
        val ast = parseToAst(file) { this.parser.parseForHeader(it) }
        return extractHeaderDetails(ast, file, titleTransformer).first
    }

    override fun convert(file: Path, context: ConvertingContext): PageContent {
        val attachmentsRegistry = AttachmentsRegistry()
        val (header, ast) = parseToHeaderAndBody(file, context, attachmentsRegistry)

        collectAttachments(file, context, ast, attachmentsRegistry)

        val generator = parser.htmlRenderer(file, attachmentsRegistry.collectedAttachments, context)
        return PageContent(
            header,
            generator.render(ast),
            attachmentsRegistry.collectedAttachments.values.toList()
        )
    }

    private fun parseToAst(file: Path, fileParser: (Reader) -> Document): Document {
        return try {
            Files.newBufferedReader(file, Charsets.UTF_8).use { fileParser(it) }
        } catch (ex: Exception) {
            throw ConversionFailedException(file, "Document parsing failed", ex)
        }
    }

    private fun collectAttachments(
        file: Path,
        context: ConvertingContext,
        ast: Document,
        attachmentsRegistry: AttachmentsRegistry
    ) {
        try {
            return AttachmentCollector(file, context.referenceProvider, attachmentsRegistry).collectAttachments(ast)
        } catch (ex: Exception) {
            throw ConversionFailedException(file, "Failed to extract attachments", ex)
        }
    }

    private fun parseToHeaderAndBody(
        file: Path,
        context: ConvertingContext,
        attachmentsRegistry: AttachmentsRegistry
    ): Pair<PageHeader, Document> {
        val ast = parseToAst(file) { parser.parseReader(it, context, attachmentsRegistry, file) }

        return extractHeaderDetails(ast, file, context.titleTransformer)
    }

    private fun extractHeaderDetails(
        ast: Document,
        file: Path,
        titleTransformer: (Path, String) -> String
    ): Pair<PageHeader, Document> {
        val attributes = readAttributes(ast)
        val title = attributes["title"]?.toString()
            ?: documentTitle(ast)
            ?: file.toFile().nameWithoutExtension
        return PageHeader(titleTransformer(file, title), attributes) to ast
    }

    private fun readAttributes(ast: Document): Map<String, Any> =
        AbstractYamlFrontMatterVisitor().let {
            it.visit(ast)
            it.data.mapValues { (_, v) -> if (v.size == 1) v.first() else v }
        }.mapValues { (_, v) ->
            if (v is String) {
                if (v.enclosedIn('{', '}')) {
                    JSON_PARSER.readValue<Map<String, *>>(v)
                } else if (v.enclosedIn('[', ']')) {
                    JSON_PARSER.readValue<List<String>>(v)
                } else {
                    v
                }
            } else {
                v
            }
        }

    private fun String.enclosedIn(start: Char, end: Char): Boolean {
        return this.startsWith(start) && this.endsWith(end)
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

    companion object {
        private val JSON_PARSER = ObjectMapper()
    }
}