package com.github.zeldigas.text2confl.convert.markdown

import assertk.assertThat
import com.github.zeldigas.text2confl.convert.Attachment
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

internal class RenderingOfImagesTest : RenderingTestBase() {

    @Test
    internal fun `Existing images rendering`() {
        val result = toHtml(
            """
            ![Alt text](https://example.org/test.jpg "A Title")
            
            ![](https://example.org/test.jpg)
            
            ![](assets/image.jpg)
            
            ![Alt](assets/image.jpg "Asset")                       
        """.trimIndent(),
            attachments = mapOf("assets/image.jpg" to Attachment("an_attachment", "assets/image.jpg", Path("assets/image.jpg")))
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p><ac:image ac:alt="Alt text" ac:title="A Title"><ri:url ri:value="https://example.org/test.jpg" /></ac:image></p>
            <p><ac:image ac:alt=""><ri:url ri:value="https://example.org/test.jpg" /></ac:image></p>
            <p><ac:image ac:alt=""><ri:attachment ri:filename="an_attachment" /></ac:image></p>
            <p><ac:image ac:alt="Alt" ac:title="Asset"><ri:attachment ri:filename="an_attachment" /></ac:image></p>
        """.trimIndent(),
        )
    }

    @Test
    internal fun `Existing images references rendering`() {
        val result = toHtml(
            """
            ![Alt text][external]           
            
            ![][attached]
            
            ![Alt][attached]
                                   
            [external]: https://example.org/test.jpg "External image"
            [attached]: assets/image.jpg "Attached image"
        """.trimIndent(),
            attachments = mapOf("assets/image.jpg" to Attachment("an_attachment", "assets/image.jpg", Path("assets/image.jpg")))
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p><ac:image ac:alt="Alt text" ac:title="External image"><ri:url ri:value="https://example.org/test.jpg" /></ac:image></p>
            <p><ac:image ac:alt="" ac:title="Attached image"><ri:attachment ri:filename="an_attachment" /></ac:image></p>
            <p><ac:image ac:alt="Alt" ac:title="Attached image"><ri:attachment ri:filename="an_attachment" /></ac:image></p>
        """.trimIndent(),
        )
    }

    @Test
    internal fun `Missing images rendering`() {
        val result = toHtml(
            """            
            ![](assets/image.jpg)
            
            ![Alt](assets/image.jpg "Asset")                       
        """.trimIndent(),
            attachments = emptyMap()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p><ac:image ac:alt=""><ri:url ri:value="assets/image.jpg" /></ac:image></p>
            <p><ac:image ac:alt="Alt" ac:title="Asset"><ri:url ri:value="assets/image.jpg" /></ac:image></p>
        """.trimIndent(),
        )
    }

    @Test
    internal fun `Missing and invalid images references rendering`() {
        val result = toHtml(
            """
            ![Alt text][external]           
            
            ![][attached]      
                  
            ![Alt][attached]
                                   
            [attached]: assets/image.jpg "Attached image"
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """
            <p>![Alt text][external]</p>
            <p><ac:image ac:alt="" ac:title="Attached image"><ri:url ri:value="assets/image.jpg" /></ac:image></p>
            <p><ac:image ac:alt="Alt" ac:title="Attached image"><ri:url ri:value="assets/image.jpg" /></ac:image></p>
        """.trimIndent(),
        )
    }

}


