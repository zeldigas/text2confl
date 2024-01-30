package com.github.zeldigas.text2confl.convert.markdown.export

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter
import com.vladsch.flexmark.util.data.DataKey
import com.vladsch.flexmark.util.data.MutableDataSet
import java.nio.file.Path
import kotlin.io.path.Path

class HtmlToMarkdownConverter(
    linkResolver: ConfluenceLinksResolver,
    assetsLocation: String,
    userResolver: ConfluenceUserResolver? = null
) {

    companion object {
        val LINK_RESOLVER =
            DataKey<ConfluenceLinksResolver>("CONFLUENCE_LINK_RESOLVER") { ConfluenceLinksResolver.NOP }
        val USER_RESOLVER =
            DataKey<ConfluenceUserResolver>("CONFLUENCE_USER_RESOLVER") { ConfluenceUserResolver.NOP }
        val ASSETS_DIR = DataKey<Path>("CONFLUENCE_ASSETS_DIR") { Path("_assets") }
    }

    private val converter = FlexmarkHtmlConverter.builder(
        MutableDataSet()
            .set(FlexmarkHtmlConverter.SETEXT_HEADINGS, false)
            .set(FlexmarkHtmlConverter.LIST_CONTENT_INDENT, false)
            .set(LINK_RESOLVER, linkResolver)
            .set(ASSETS_DIR, Path(assetsLocation))
            .also {
                if (userResolver != null) {
                    it.set(USER_RESOLVER, userResolver)
                }
            }
    )
        .htmlNodeRendererFactory { ConfluenceCustomNodeRenderer(it) }
        .build()

    fun convert(content: String): String {
        return converter.convert(content)
    }

}