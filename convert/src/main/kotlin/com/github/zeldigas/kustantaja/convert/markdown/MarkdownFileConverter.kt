package com.github.zeldigas.kustantaja.convert.markdown

import com.github.zeldigas.kustantaja.convert.*
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughSubscriptExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.KeepType
import com.vladsch.flexmark.util.data.DataHolder
import com.vladsch.flexmark.util.data.MutableDataSet
import java.nio.file.Files
import java.nio.file.Path


class MarkdownFileConverter : FileConverter {

    private val standardExtensions = listOf(
        TablesExtension.create(), YamlFrontMatterExtension.create(),
        TaskListExtension.create(), StrikethroughSubscriptExtension.create()
    )
    private val parserOptions: DataHolder = MutableDataSet()
        .set(Parser.REFERENCES_KEEP, KeepType.LAST)
        .set(HtmlRenderer.INDENT_SIZE, 2)
        .set(HtmlRenderer.PERCENT_ENCODE_URLS, true)

        // for full GFM table compatibility add the following table extension options:
        .set(TablesExtension.COLUMN_SPANS, false)
        .set(TablesExtension.APPEND_MISSING_COLUMNS, true)
        .set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
        .set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true)
        .set(
            Parser.EXTENSIONS, standardExtensions
        )
        .toImmutable()

    private val parser = Parser.builder(parserOptions).build()

    override fun readHeader(file: Path): PageHeader {
        val ast = Files.newBufferedReader(file, Charsets.UTF_8).use { parser.parseReader(it) }

        val attributes = readAttributes(ast)
        return createHeader(attributes, file)
    }

    override fun convert(file: Path, context: ConvertingContext): PageContent {
        val ast = Files.newBufferedReader(file, Charsets.UTF_8).use { parser.parseReader(it) }

        val attributes = readAttributes(ast)
        val attachments = AttachmentCollector(file).collectAttachments(ast)
            .map { (name, path) -> name to Attachment.fromLink(name, path) }.toMap()
        val generator = htmlRenderer(attachments, context)
        return PageContent(
            createHeader(attributes, file),
            generator.render(ast),
            attachments.values.toList()
        )
    }

    private fun htmlRenderer(attachments: Map<String, Attachment>, context: ConvertingContext): HtmlRenderer {
        return HtmlRenderer.builder(
            parserOptions.toMutable()
                .set(Parser.EXTENSIONS, listOf(ConfluenceFormatExtension()) + standardExtensions)
                .set(ConfluenceFormatExtension.ATTACHMENTS, attachments)
                .set(ConfluenceFormatExtension.CONTEXT, context)
                .toImmutable()
        ).build()
    }

    private fun createHeader(
        attributes: Map<String, Any>,
        file: Path
    ) = PageHeader(attributes["title"]?.toString() ?: file.toFile().nameWithoutExtension, attributes)

    private fun readAttributes(ast: Document): Map<String, Any> {
        val attributes = AbstractYamlFrontMatterVisitor().let {
            it.visit(ast)
            it.data.mapValues { (_, v) -> if (v.size == 1) v.first() else v }
        }
        return attributes
    }
}