package com.github.zeldigas.text2confl.convert.markdown

import com.github.zeldigas.text2confl.convert.*
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.util.ast.Document
import java.nio.file.Files
import java.nio.file.Path


class MarkdownFileConverter : FileConverter {

    private val parser: MarkdownParser = MarkdownParser()

    override fun readHeader(file: Path, context: HeaderReadingContext): PageHeader {
        val ast = Files.newBufferedReader(file, Charsets.UTF_8).use { parser.parseReader(it) }

        val attributes = readAttributes(ast)
        return createHeader(attributes, file, context.titleTransformer)
    }

    override fun convert(file: Path, context: ConvertingContext): PageContent {
        val ast = Files.newBufferedReader(file, Charsets.UTF_8).use { parser.parseReader(it) }

        val attributes = readAttributes(ast)
        val attachments = AttachmentCollector(file, context.referenceProvider).collectAttachments(ast)
            .map { (name, path) -> name to Attachment.fromLink(name, path) }.toMap()
        val generator = parser.htmlRenderer(file, attachments, context)
        return PageContent(
            createHeader(attributes, file, context.titleTransformer),
            generator.render(ast),
            attachments.values.toList()
        )
    }

    private fun createHeader(
        attributes: Map<String, Any>,
        file: Path,
        titleTransformer: (Path, String) -> String
    ): PageHeader {
        val title = attributes["title"]?.toString() ?: file.toFile().nameWithoutExtension
        return PageHeader(titleTransformer(file, title), attributes)
    }

    private fun readAttributes(ast: Document): Map<String, Any> {
        val attributes = AbstractYamlFrontMatterVisitor().let {
            it.visit(ast)
            it.data.mapValues { (_, v) -> if (v.size == 1) v.first() else v }
        }
        return attributes
    }
}