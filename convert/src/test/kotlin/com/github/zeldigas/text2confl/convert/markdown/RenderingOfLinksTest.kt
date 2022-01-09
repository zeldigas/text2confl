package com.github.zeldigas.text2confl.convert.markdown

import assertk.assertThat
import com.github.zeldigas.text2confl.convert.Attachment
import com.github.zeldigas.text2confl.convert.PageHeader
import com.github.zeldigas.text2confl.convert.confluence.ReferenceProvider
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

internal class RenderingOfLinksTest : RenderingTestBase() {

    @Test
    internal fun `Cross references rendering`() {
        val result = toHtml(
            """
            [Link](another.md)

            [Link with anchor](another.md#test)
            
            [Link with anchor to another type](test/another.adoc#first-header)
            
            [Link ~~with~~ **anchor** to `another type`](test/another.adoc#first-header)
            
            [Link with anchor][ref1]
            
            [Link ~~with~~ **anchor** to `another type`][ref2]
            
            [ref1]: another.md#test
            [ref2]: test/another.adoc#first-header
        """.trimIndent(),
            referenceProvider = ReferenceProvider.fromDocuments(Path("."), mapOf(
                Path("src.md") to PageHeader("Test", emptyMap()),
                Path("another.md") to PageHeader("Another md", emptyMap()),
                Path("test/another.adoc") to PageHeader("Asciidoc", emptyMap())
            ))
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p><ac:link><ri:page ri:content-title="Another md" ri:space-key="TEST" /><ac:plain-text-link-body><![CDATA[Link]]></ac:plain-text-link-body></ac:link></p>
            <p><ac:link ac:anchor="test"><ri:page ri:content-title="Another md" ri:space-key="TEST" /><ac:plain-text-link-body><![CDATA[Link with anchor]]></ac:plain-text-link-body></ac:link></p>
            <p><ac:link ac:anchor="first-header"><ri:page ri:content-title="Asciidoc" ri:space-key="TEST" /><ac:plain-text-link-body><![CDATA[Link with anchor to another type]]></ac:plain-text-link-body></ac:link></p>
            <p><ac:link ac:anchor="first-header"><ri:page ri:content-title="Asciidoc" ri:space-key="TEST" /><ac:link-body>Link <del>with</del> <strong>anchor</strong> to <code>another type</code></ac:link-body></ac:link></p>
            <p><ac:link ac:anchor="test"><ri:page ri:content-title="Another md" ri:space-key="TEST" /><ac:plain-text-link-body><![CDATA[Link with anchor]]></ac:plain-text-link-body></ac:link></p>
            <p><ac:link ac:anchor="first-header"><ri:page ri:content-title="Asciidoc" ri:space-key="TEST" /><ac:link-body>Link <del>with</del> <strong>anchor</strong> to <code>another type</code></ac:link-body></ac:link></p>
        """.trimIndent(),
        )
    }

    @Test
    internal fun `Anchors references rendering`() {
        val result = toHtml(
            """
            [Link](#test)
            
            [Link ~~with~~ **anchor** to `another type`](#another-anchor)
            
            [Link][anch1]
            
            [Link ~~with~~ **anchor** to `another type`][anch2]
            
            [anch1]: #test
            [anch2]: #another-anchor
        """.trimIndent(),
            referenceProvider = ReferenceProvider.fromDocuments(Path("."), mapOf(
                Path("src.md") to PageHeader("Test", emptyMap())
            ))

        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p><ac:link ac:anchor="test"><ac:plain-text-link-body><![CDATA[Link]]></ac:plain-text-link-body></ac:link></p>
            <p><ac:link ac:anchor="another-anchor"><ac:link-body>Link <del>with</del> <strong>anchor</strong> to <code>another type</code></ac:link-body></ac:link></p>
            <p><ac:link ac:anchor="test"><ac:plain-text-link-body><![CDATA[Link]]></ac:plain-text-link-body></ac:link></p>
            <p><ac:link ac:anchor="another-anchor"><ac:link-body>Link <del>with</del> <strong>anchor</strong> to <code>another type</code></ac:link-body></ac:link></p>
        """.trimIndent(),
        )
    }

    @Test
    internal fun `Attachments rendering`() {
        val result = toHtml(
            """
            [attached file](assets/test.txt "Ignored")
            
            [attached `code` **formatting**](assets/test.txt "Ignored")
            
            [non-existing file](assets/missing.mp4 "Ignored")
            
            [Alt][attached]
            
            [Alt][missing]
            
            [Alt][broken]
                                   
            [attached]: assets/test.txt "Attached file"
            [missing]: assets/missing.mp4
        """.trimIndent(),
            attachments = mapOf(
                "assets/test.txt" to Attachment("an_attachment", "assets/test.txt", Path("assets/test.txt"))
            )
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p><ac:link><ri:attachment ri:filename="an_attachment" /><ac:plain-text-link-body><![CDATA[attached file]]></ac:plain-text-link-body></ac:link></p>
            <p><ac:link><ri:attachment ri:filename="an_attachment" /><ac:link-body>attached <code>code</code> <strong>formatting</strong></ac:link-body></ac:link></p>
            <p><a href="assets/missing.mp4" title="Ignored">non-existing file</a></p>
            <p><ac:link><ri:attachment ri:filename="an_attachment" /><ac:plain-text-link-body><![CDATA[attached file]]></ac:plain-text-link-body></ac:link></p>
            <p><a href="assets/missing.mp4">Alt</a></p>
            <p>[Alt][broken]</p>
        """.trimIndent(),
        )
    }

    @Test
    internal fun `Simple links rendering`() {
        val result = toHtml(
            """            
            Strong **[Strong](https://example.org)**.
            *[Markdown Guide](https://example.org/italics)*.
            Mixed content [`code` and **strong** and *italic*](https://example.org/mixed)
                                   
            Link [~~strikethrough~~][ext]
            
            [broken][broken-link]
            
            [ext]: https://example.org/external
        """.trimIndent(),
            attachments = emptyMap()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p>Strong <strong><a href="https://example.org">Strong</a></strong>.
            <em><a href="https://example.org/italics">Markdown Guide</a></em>.
            Mixed content <a href="https://example.org/mixed"><code>code</code> and <strong>strong</strong> and <em>italic</em></a></p>
            <p>Link <a href="https://example.org/external"><del>strikethrough</del></a></p>
            <p>[broken][broken-link]</p>
        """.trimIndent(),
        )
    }

}


