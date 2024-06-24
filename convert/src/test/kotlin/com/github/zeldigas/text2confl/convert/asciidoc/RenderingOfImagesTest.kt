package com.github.zeldigas.text2confl.convert.asciidoc

import assertk.assertThat
import com.github.zeldigas.text2confl.convert.Attachment
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

internal class RenderingOfImagesTest : RenderingTestBase() {

    @Test
    internal fun `Existing images block rendering`() {
        val result = toHtml(
            """
            .A Title    
            image::https://example.org/test.jpg[Alt text]
            
            image::https://example.org/test.jpg?id=12&param=b[]
            
            image::assets/image.jpg[]
            
            [#img_custom_id]
            ."Quoted text" regular text <special text>
            image::assets/image.jpg[Alt]                       
        """.trimIndent(),
            attachments = mapOf(
                "assets/image.jpg" to Attachment(
                    "an_attachment",
                    "assets/image.jpg",
                    Path("assets/image.jpg")
                )
            )
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p><ac:image ac:title="A Title" ac:alt="Alt text"><ri:url ri:value="https://example.org/test.jpg" /></ac:image><div class="t2c-image-title"><em>Figure 1. A Title</em></div></p>
            <p><ac:image ac:alt="test"><ri:url ri:value="https://example.org/test.jpg?id=12&amp;param=b" /></ac:image></p>
            <p><ac:image ac:alt="image"><ri:attachment ri:filename="an_attachment" /></ac:image></p>
            <p><ac:image ac:title="&quot;Quoted text&quot; regular text &lt;special text&gt;" ac:alt="Alt"><ri:attachment ri:filename="an_attachment" /></ac:image><div class="t2c-image-title"><em>Figure 2. "Quoted text" regular text &lt;special text&gt;</em></div></p>
        """.trimIndent(),
        )
    }

    @Test
    internal fun `Existing images inline rendering`() {
        val result = toHtml(
            """
            External image inside paragraph - image:https://example.org/test.jpg?id=12&param=b[Alt text,title="A Title"]
            
            Attachment image inside paragraph - image:assets/image.jpg[Alt,title="Asset"]
        """.trimIndent(),
            attachments = mapOf(
                "assets/image.jpg" to Attachment(
                    "an_attachment",
                    "assets/image.jpg",
                    Path("assets/image.jpg")
                )
            )
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p>External image inside paragraph - <ac:image ac:title="A Title" ac:alt="Alt text"><ri:url ri:value="https://example.org/test.jpg?id=12&amp;param=b" /></ac:image></p>
            <p>Attachment image inside paragraph - <ac:image ac:title="Asset" ac:alt="Alt"><ri:attachment ri:filename="an_attachment" /></ac:image></p>
        """.trimIndent(),
        )
    }

    @Test
    internal fun `Missing images block rendering`() {
        val result = toHtml(
            """            
            image::assets/image.jpg[]
            
            .Asset
            image::assets/image.jpg[Alt]               
        """.trimIndent(),
            attachments = emptyMap()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p><ac:image ac:alt="image"><ri:url ri:value="assets/image.jpg" /></ac:image></p>
            <p><ac:image ac:title="Asset" ac:alt="Alt"><ri:url ri:value="assets/image.jpg" /></ac:image><div class="t2c-image-title"><em>Figure 1. Asset</em></div></p>
        """.trimIndent(),
        )
    }

    @Test
    internal fun `Missing images inline rendering`() {
        val result = toHtml(
            """            
            Image 1 - image:assets/image.jpg[]
            
            Image 2 - image:assets/image.jpg[Alt, title="Asset"]               
        """.trimIndent(),
            attachments = emptyMap()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p>Image 1 - <ac:image ac:alt="image"><ri:url ri:value="assets/image.jpg" /></ac:image></p>
            <p>Image 2 - <ac:image ac:title="Asset" ac:alt="Alt"><ri:url ri:value="assets/image.jpg" /></ac:image></p>
        """.trimIndent(),
        )
    }

    @Test
    internal fun `Image rendering with extra attributes`() {
        val result = toHtml(
            """
            image::https://example.org/test.jpg[Alt text,title="A title",align=left,border=true,imgstyle="font-weight=bold;",class="custom"]
            
            Local image - image:assets/image.jpg[Alt,100,250,title="Asset",thumbnail=true,vspace=10,hspace=5,unsupported=hello,queryparams="effects=border-simple,blur-border,tape"]
        """.trimIndent(),
            attachments = mapOf(
                "assets/image.jpg" to Attachment(
                    "an_attachment",
                    "assets/image.jpg",
                    Path("assets/image.jpg")
                )
            )
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p><ac:image ac:title="A title" ac:alt="Alt text" ac:align="left" ac:border="true" ac:class="custom" ac:style="font-weight=bold;"><ri:url ri:value="https://example.org/test.jpg" /></ac:image><div class="t2c-image-title"><em>Figure 1. A title</em></div></p>
            <p>Local image - <ac:image ac:height="250" ac:width="100" ac:title="Asset" ac:alt="Alt" ac:thumbnail="true" ac:vspace="10" ac:hspace="5" ac:queryparams="effects=border-simple,blur-border,tape"><ri:attachment ri:filename="an_attachment" /></ac:image></p>
        """.trimIndent(),
        )
    }

}


