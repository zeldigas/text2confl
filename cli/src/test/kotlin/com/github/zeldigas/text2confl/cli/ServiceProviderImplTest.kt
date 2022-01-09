package com.github.zeldigas.text2confl.cli

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import assertk.assertions.prop
import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.confclient.ConfluenceClientConfig
import com.github.zeldigas.confclient.TokenAuth
import com.github.zeldigas.confclient.confluenceClient
import com.github.zeldigas.text2confl.cli.config.ConverterConfig
import com.github.zeldigas.text2confl.cli.config.EditorVersion
import com.github.zeldigas.text2confl.cli.config.UploadConfig
import com.github.zeldigas.text2confl.convert.Converter
import com.github.zeldigas.text2confl.convert.universalConverter
import io.ktor.http.*
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.io.path.Path

@ExtendWith(MockKExtension::class)
internal class ServiceProviderImplTest {

    private val provider = ServiceProviderImpl()

    @Test
    internal fun `Content uploader creation`(
        @MockK client: ConfluenceClient
    ) {
        val result = provider.createUploader(
            client,
            UploadConfig("TEST", false, "test", true, ChangeDetector.CONTENT),
            ConverterConfig("pre", "post", EditorVersion.V2)
        )

        assertThat(result).all {
            prop(ContentUploader::client).isEqualTo(client)
            prop(ContentUploader::editorVersion).isEqualTo(EditorVersion.V2)
            prop(ContentUploader::notifyWatchers).isTrue()
            prop(ContentUploader::pageContentChangeDetector).isEqualTo(ChangeDetector.CONTENT)
            prop(ContentUploader::uploadMessage).isEqualTo("test")
        }
    }

    @Test
    internal fun `Confluence client creation`(@MockK client: ConfluenceClient) {
        mockkStatic(::confluenceClient) {
            val config = ConfluenceClientConfig(Url("https://example.org"), false, TokenAuth("test"))

            every { confluenceClient(config) } returns client

            val result = provider.createConfluenceClient(config)

            assertThat(result).isEqualTo(client)
        }
    }

    @Test
    internal fun `Converter creation`(@MockK converter: Converter) {
        mockkStatic(::universalConverter) {
            every { universalConverter("TEST", any(), any()) } returns converter

            val result = provider.createConverter("TEST", ConverterConfig("pre", "post", EditorVersion.V1))

            assertThat(result).isEqualTo(converter)

            verify { universalConverter("TEST", any(), match { it(Path("."), "Test") == "preTestpost" }) }
        }
    }
}