package com.github.zeldigas.text2confl.convert.markdown

import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.ConvertingContext
import com.github.zeldigas.text2confl.convert.markdown.ext.SimpleAdmonitionExtension
import com.github.zeldigas.text2confl.convert.markdown.ext.SimpleAttributesExtension
import com.github.zeldigas.text2confl.convert.markdown.ext.SimpleMacroExtension
import com.vladsch.flexmark.ext.attributes.AttributesExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughSubscriptExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension
import com.vladsch.flexmark.ext.superscript.SuperscriptExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.toc.TocExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.KeepType
import com.vladsch.flexmark.util.data.DataHolder
import com.vladsch.flexmark.util.data.MutableDataSet
import com.vladsch.flexmark.util.data.NullableDataKey
import java.io.BufferedReader
import java.nio.file.Path

internal class MarkdownParser(config: MarkdownConfiguration) {

    companion object {
        val PARSE_OPTIONS = NullableDataKey<MarkdownConfiguration>("T2C_CONFIG")
    }

    private val parserOptions: DataHolder = MutableDataSet()
        .set(Parser.REFERENCES_KEEP, KeepType.LAST)
        .set(Parser.HTML_BLOCK_DEEP_PARSER, true)
        .set(HtmlRenderer.RENDER_HEADER_ID, true)
        .set(HtmlRenderer.INDENT_SIZE, 2)
        .set(HtmlRenderer.PERCENT_ENCODE_URLS, true)
        .set(HtmlRenderer.SOFT_BREAK, " ")
        .set(AttributesExtension.FENCED_CODE_INFO_ATTRIBUTES, true)
        // for full GFM table compatibility add the following table extension options:
        .set(TablesExtension.COLUMN_SPANS, false)
        .set(TablesExtension.APPEND_MISSING_COLUMNS, true)
        .set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
        .set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true)
        .set(PARSE_OPTIONS, config)
        .set(
            Parser.EXTENSIONS, listOf(
                TablesExtension.create(), YamlFrontMatterExtension.create(),
                TaskListExtension.create(), StrikethroughSubscriptExtension.create(),
                SimpleAttributesExtension(), TocExtension.create(),
                SimpleAdmonitionExtension(), SuperscriptExtension.create(),
                StatusExtension(), ConfluenceUserExtension(),
                SimpleMacroExtension(),
                ConfluenceFormatExtension(),

            )
        )
        .toImmutable()

    private val parser = Parser.builder(parserOptions).build()

    fun parseReader(reader: BufferedReader): Document {
        return parser.parseReader(reader)
    }

    fun parseString(document: String): Document {
        return parser.parse(document)
    }

    fun htmlRenderer(location: Path, attachments: Map<String, Attachment>, context: ConvertingContext): HtmlRenderer {
        return HtmlRenderer.builder(
            parserOptions.toMutable()
                .set(ConfluenceFormatExtension.DOCUMENT_LOCATION, location)
                .set(ConfluenceFormatExtension.ATTACHMENTS, attachments)
                .set(ConfluenceFormatExtension.CONTEXT, context)
                .toImmutable()
        ).build()
    }


}