package com.github.zeldigas.text2confl.convert.markdown

import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.AttachmentsRegistry
import com.github.zeldigas.text2confl.convert.ConvertingContext
import com.github.zeldigas.text2confl.convert.markdown.diagram.DiagramMakers
import com.github.zeldigas.text2confl.convert.markdown.diagram.DiagramsExtension
import com.github.zeldigas.text2confl.convert.markdown.ext.SimpleAdmonitionExtension
import com.github.zeldigas.text2confl.convert.markdown.ext.SimpleAttributesExtension
import com.github.zeldigas.text2confl.convert.markdown.ext.SimpleMacroExtension
import com.vladsch.flexmark.ext.attributes.AttributesExtension
import com.vladsch.flexmark.ext.emoji.EmojiExtension
import com.vladsch.flexmark.ext.emoji.EmojiImageType
import com.vladsch.flexmark.ext.emoji.EmojiShortcutType
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
import com.vladsch.flexmark.util.misc.Extension
import java.io.Reader
import java.nio.file.Path

internal class MarkdownParser(config: MarkdownConfiguration, diagramMakers: DiagramMakers = DiagramMakers.NOP) {

    companion object {
        val PARSE_OPTIONS = NullableDataKey<MarkdownConfiguration>("T2C_CONFIG")
        val CONTEXT = NullableDataKey<ConvertingContext>("T2C_CONVERTING_CONTEXT", null)
        val DOCUMENT_LOCATION = NullableDataKey<Path>("T2C_DOCUMENT_LOCATION", null)
        val ATTACHMENTS_REGISTRY = NullableDataKey<AttachmentsRegistry>("T2C_ATTACHMENTS_REGISTRY", null)
    }

    private val headerParserOptions = MutableDataSet()
        .set(Parser.EXTENSIONS, listOf(
            YamlFrontMatterExtension.create()
        ))
        .toImmutable()

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
        .set(DiagramsExtension.DIAGRAM_MAKERS, diagramMakers)
        .set(PARSE_OPTIONS, config).let { parserConfig ->
            parserConfig.set(Parser.EXTENSIONS, listOf(
                TablesExtension.create(), YamlFrontMatterExtension.create(),
                TaskListExtension.create(), StrikethroughSubscriptExtension.create(),
                SimpleAttributesExtension(), TocExtension.create(),
                SimpleAdmonitionExtension(), SuperscriptExtension.create(),
                StatusExtension(), ConfluenceUserExtension(),
                SimpleMacroExtension(),
                DiagramsExtension(),
                ConfluenceFormatExtension(),
            ) + extraExtensions(parserConfig, config))
        }
        .toImmutable()

    private fun extraExtensions(parserConfig: MutableDataSet, config: MarkdownConfiguration): List<Extension> {
        return buildList {
            if (config.emoji) {
                add(EmojiExtension.create())
                parserConfig.set(EmojiExtension.USE_SHORTCUT_TYPE, EmojiShortcutType.ANY_EMOJI_CHEAT_SHEET_PREFERRED)
                parserConfig.set(EmojiExtension.USE_IMAGE_TYPE, EmojiImageType.UNICODE_ONLY)
            }
        }
    }

    private val headerParser = Parser.builder(headerParserOptions).build()
    private val parser = Parser.builder(parserOptions).build()

    fun parseReader(reader: Reader,
                    context: ConvertingContext,
                    attachmentsRegistry: AttachmentsRegistry,
                    location: Path): Document {
        return createParser(context, attachmentsRegistry, location).parseReader(reader)
    }

    private fun createParser(context: ConvertingContext, attachmentsRegistry: AttachmentsRegistry, location: Path): Parser {
        return Parser.builder(parserOptions.toMutable()
            .set(ATTACHMENTS_REGISTRY, attachmentsRegistry)
            .set(CONTEXT, context)
            .set(DOCUMENT_LOCATION, location)
        ).build()
    }

    fun parseString(document: String,
                    context: ConvertingContext,
                    attachmentsRegistry: AttachmentsRegistry,
                    location: Path): Document {
        return createParser(context, attachmentsRegistry, location).parse(document)
    }

    fun htmlRenderer(location: Path, attachments: Map<String, Attachment>, context: ConvertingContext): HtmlRenderer {
        return HtmlRenderer.builder(
            parserOptions.toMutable()
                .set(DOCUMENT_LOCATION, location)
                .set(ConfluenceFormatExtension.ATTACHMENTS, attachments)
                .set(CONTEXT, context)
                .toImmutable()
        ).build()
    }

    fun parseForHeader(reader: Reader): Document {
        return headerParser.parseReader(reader)
    }

}