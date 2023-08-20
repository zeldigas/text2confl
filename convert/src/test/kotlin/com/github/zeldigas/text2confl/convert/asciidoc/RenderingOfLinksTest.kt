package com.github.zeldigas.text2confl.convert.asciidoc

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
            xref:another.adoc[]
            
            xref:another.adoc[Link]

            xref:another.adoc#test[Link with anchor]
            
            link:another.adoc[]
            
            xref:test/another.md#first-header[Link with anchor to another type]
            
            link:test/another.md#first-header[Link with anchor to another type via link macro]
            
            xref:test/another.md#first-header[Link [.line-through]#with# **anchor** to `another type`]                       
        """.trimIndent(),
            referenceProvider = ReferenceProvider.fromDocuments(Path("."), mapOf(
                Path("src.adoc") to PageHeader("Test", emptyMap()),
                Path("another.adoc") to PageHeader("Another adoc", emptyMap()),
                Path("test/another.md") to PageHeader("Markdown", emptyMap())
            ))
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p><ac:link><ri:page ri:content-title="Another adoc" ri:space-key="TEST" /><ac:plain-text-link-body><![CDATA[Another adoc]]></ac:plain-text-link-body></ac:link></p>
            <p><ac:link><ri:page ri:content-title="Another adoc" ri:space-key="TEST" /><ac:plain-text-link-body><![CDATA[Link]]></ac:plain-text-link-body></ac:link></p>
            <p><ac:link ac:anchor="test"><ri:page ri:content-title="Another adoc" ri:space-key="TEST" /><ac:plain-text-link-body><![CDATA[Link with anchor]]></ac:plain-text-link-body></ac:link></p>
            <p><ac:link><ri:page ri:content-title="Another adoc" ri:space-key="TEST" /><ac:plain-text-link-body><![CDATA[Another adoc]]></ac:plain-text-link-body></ac:link></p>
            <p><ac:link ac:anchor="first-header"><ri:page ri:content-title="Markdown" ri:space-key="TEST" /><ac:plain-text-link-body><![CDATA[Link with anchor to another type]]></ac:plain-text-link-body></ac:link></p>
            <p><ac:link ac:anchor="first-header"><ri:page ri:content-title="Markdown" ri:space-key="TEST" /><ac:plain-text-link-body><![CDATA[Link with anchor to another type via link macro]]></ac:plain-text-link-body></ac:link></p>
            <p><ac:link ac:anchor="first-header"><ri:page ri:content-title="Markdown" ri:space-key="TEST" /><ac:link-body>Link <del>with</del> <strong>anchor</strong> to <code>another type</code></ac:link-body></ac:link></p>
        """.trimIndent(),
        )
    }

    @Test
    internal fun `Anchors references rendering`() {
        val result = toHtml(
            """
            <<test,Link>>
            
            <<another-anchor,Link [.line-through]#with# **anchor** to `another type`>>
            
            <<missing-anchor,Missing anchor>>
            
            <<test2>>
            
            [#test]
            Paragraph with test anchor
            
            [#test2,reftext=Custom name]
            Paragraph with customized anchor
            
            [#another-anchor]
            Another paragraph
        """.trimIndent(),
            referenceProvider = ReferenceProvider.fromDocuments(Path("."), mapOf(
                Path("src.md") to PageHeader("Test", emptyMap())
            ))

        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p><ac:link ac:anchor="test"><ac:plain-text-link-body><![CDATA[Link]]></ac:plain-text-link-body></ac:link></p>
            <p><ac:link ac:anchor="another-anchor"><ac:link-body>Link <del>with</del> <strong>anchor</strong> to <code>another type</code></ac:link-body></ac:link></p>
            <p><ac:link ac:anchor="missing-anchor"><ac:plain-text-link-body><![CDATA[Missing anchor]]></ac:plain-text-link-body></ac:link></p>
            <p><ac:link ac:anchor="test2"><ac:plain-text-link-body><![CDATA[Custom name]]></ac:plain-text-link-body></ac:link></p>
            <p><ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">test</ac:parameter></ac:structured-macro>Paragraph with test anchor</p>
            <p><ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">test2</ac:parameter></ac:structured-macro>Paragraph with customized anchor</p>
            <p><ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">another-anchor</ac:parameter></ac:structured-macro>Another paragraph</p>
        """.trimIndent(),
        )
    }

    @Test
    internal fun `Attachments rendering`() {
        val result = toHtml(
            """
            link:assets/test.txt[attached file]
            
            link:assets/test.txt[attached `code` **formatting**]
            
            link:assets/missing.mp4[non-existing file,title="Not ignored"]         
        """.trimIndent(),
            attachments = mapOf(
                "assets/test.txt" to Attachment("an_attachment", "assets/test.txt", Path("assets/test.txt"))
            )
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p><ac:link><ri:attachment ri:filename="an_attachment" /><ac:plain-text-link-body><![CDATA[attached file]]></ac:plain-text-link-body></ac:link></p>
            <p><ac:link><ri:attachment ri:filename="an_attachment" /><ac:link-body>attached <code>code</code> <strong>formatting</strong></ac:link-body></ac:link></p>
            <p><a href="assets/missing.mp4" title="Not ignored">non-existing file</a></p>
        """.trimIndent(),
        )
    }

    @Test
    internal fun `Simple links rendering`() {
        val result = toHtml(
            """
            :ext: https://example.org/external
            Strong **link:https://example.org[Strong]**. 
            _link:https://example.org/italics[Markdown Guide]_.
            Mixed content link:https://example.org/mixed[`code` and **strong** and _italic_]
                                   
            Link {ext}[+++<del>strikethrough</del>+++]
                       
            Link {ext}[[.line-through]#abc#]           
        """.trimIndent(),
            attachments = emptyMap()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p>Strong <strong><a href="https://example.org">Strong</a></strong>.
            <em><a href="https://example.org/italics">Markdown Guide</a></em>.
            Mixed content <a href="https://example.org/mixed"><code>code</code> and <strong>strong</strong> and <em>italic</em></a></p>
            <p>Link <a href="https://example.org/external"><del>strikethrough</del></a></p>
            <p>Link <a href="https://example.org/external"><del>abc</del></a></p>
        """.trimIndent(),
        )
    }

}


