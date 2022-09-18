package com.github.zeldigas.text2confl.convert

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.writeBytes

class PageContentTest {

    @Test
    internal fun `Hash of body`() {
        val content = PageContent(
            PageHeader("title", emptyMap()),
            "test", emptyList()
        )

        assertThat(content.hash).isEqualTo("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08")
    }

    @Test
    internal fun `Hash of attachment`(@TempDir dir: Path) {
        val file = dir.resolve("test.txt")

        file.writeBytes(ByteArray(50) { (it + 10).toByte() })

        val attachment = Attachment.fromLink("assets/test.txt", file)

        assertThat(attachment.hash).isEqualTo("296ca9e3e0a8505750b278fda27547660fae7d92842e8f4582b73304f6e66e0e")
    }

    @CsvSource(
        value = [
            "assets/test.txt,assets_test.txt",
            "./test.txt,test.txt",
            "../assets/test.txt,__assets_test.txt",
            "../../assets/test.txt,____assets_test.txt",
        ]
    )
    @ParameterizedTest
    internal fun `Attachment conversion`(path: String, name: String) {
        val file = Path("test.txt")

        assertThat(Attachment.fromLink(path, file)).all {
            prop(Attachment::attachmentName).isEqualTo(name)
            prop(Attachment::linkName).isEqualTo(path)
        }
    }

    @Test
    internal fun `Ok result for well-formed xml`() {
        assertThat(
            PageContent(PageHeader("", emptyMap()),
            """
                <p>hello world</p>                
            """.trimIndent(),
            emptyList()
            ).validate()
        ).isEqualTo(Validation.Ok)
    }

    @Test
    internal fun `Invalid result for unbalanced xml`() {
        val sampleXml = """
                <table>    
                <p ac:parameter="hello">hello world</p>                                
                <p>hello world &lt;/p>                
                <p>hello world</p>
                </table>""".trimIndent()
        print(sampleXml)
        assertThat(
            PageContent(PageHeader("", emptyMap()),
                sampleXml,
                emptyList()
            ).validate()
        ).isInstanceOf(Validation.Invalid::class)
            .transform { it.issue }.contains("[5:3] The element type \"p\" must be terminated by the matching end-tag \"</p>\". Start tag location - [3:4]")
    }
}