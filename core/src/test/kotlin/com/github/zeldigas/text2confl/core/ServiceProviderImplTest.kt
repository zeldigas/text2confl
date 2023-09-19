package com.github.zeldigas.text2confl.core

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.confclient.ConfluenceClientConfig
import com.github.zeldigas.confclient.TokenAuth
import com.github.zeldigas.confclient.confluenceClient
import com.github.zeldigas.text2confl.convert.ConversionParameters
import com.github.zeldigas.text2confl.convert.Converter
import com.github.zeldigas.text2confl.convert.EditorVersion
import com.github.zeldigas.text2confl.convert.asciidoc.AsciidoctorConfiguration
import com.github.zeldigas.text2confl.convert.markdown.MarkdownConfiguration
import com.github.zeldigas.text2confl.convert.universalConverter
import com.github.zeldigas.text2confl.core.config.Cleanup
import com.github.zeldigas.text2confl.core.config.CodeBlockParams
import com.github.zeldigas.text2confl.core.config.ConverterConfig
import com.github.zeldigas.text2confl.core.config.UploadConfig
import com.github.zeldigas.text2confl.core.upload.ChangeDetector
import com.github.zeldigas.text2confl.core.upload.ContentUploader
import com.github.zeldigas.text2confl.core.upload.DryRunClient
import com.github.zeldigas.text2confl.core.upload.PageUploadOperationsImpl
import io.ktor.http.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
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
            UploadConfig("TEST", Cleanup.Managed, "test", true, ChangeDetector.CONTENT, "test"),
            ConverterConfig(
                "pre", "post", EditorVersion.V2, null,
                "root/", null,
                CodeBlockParams(),
                MarkdownConfiguration(),
                AsciidoctorConfiguration()
            )
        )

        assertThat(result).all {
            prop(ContentUploader::tenant).isEqualTo("test")
            prop(ContentUploader::pageUploadOperations).isInstanceOf(PageUploadOperationsImpl::class).all {
                prop(PageUploadOperationsImpl::client).isEqualTo(client)
                prop(PageUploadOperationsImpl::editorVersion).isEqualTo(EditorVersion.V2)
                prop(PageUploadOperationsImpl::notifyWatchers).isTrue()
                prop(PageUploadOperationsImpl::pageContentChangeDetector).isEqualTo(ChangeDetector.CONTENT)
                prop(PageUploadOperationsImpl::uploadMessage).isEqualTo("test")
                prop(PageUploadOperationsImpl::tenant).isEqualTo("test")
            }
        }
    }

    @Test
    internal fun `Confluence client creation`(@MockK client: ConfluenceClient) {
        mockkStatic(::confluenceClient) {
            val config = ConfluenceClientConfig(Url("https://example.org"), false, TokenAuth("test"))

            every { confluenceClient(config) } returns client

            val result = provider.createConfluenceClient(config, false)

            assertThat(result).isEqualTo(client)
        }
    }

    @Test
    internal fun `Confluence dry client creation`(@MockK client: ConfluenceClient) {
        mockkStatic(::confluenceClient) {
            every { confluenceClient(any()) } returns client

            val result = provider.createConfluenceClient(mockk(), true)

            assertThat(result).isInstanceOf(DryRunClient::class)
        }
    }

    @Test
    internal fun `Converter creation`(@MockK converter: Converter) {
        mockkStatic(::universalConverter) {
            every { universalConverter("TEST", any()) } returns converter

            val result = provider.createConverter(
                "TEST", ConverterConfig(
                    "pre", "post", EditorVersion.V1,
                    null, "http://example.org/", "custom text",
                    CodeBlockParams(), MarkdownConfiguration(), AsciidoctorConfiguration()
                )
            )

            assertThat(result).isEqualTo(converter)

            val configSlot = slot<ConversionParameters>()
            verify { universalConverter("TEST", capture(configSlot)) }

            assertThat(configSlot.captured).all {
                prop(ConversionParameters::titleConverter).transform { it(Path("."), "Test") }.isEqualTo("preTestpost")
                prop(ConversionParameters::addAutogeneratedNote).isTrue()
                prop(ConversionParameters::docRootLocation).isEqualTo("http://example.org/")
            }
        }
    }

    @Test
    fun `Page exporter creation`() {
        val client = mockk<ConfluenceClient>()

        val result = provider.createPageExporter(client, true)

        assertThat(result.client).isSameAs(client)
        assertThat(result.saveContentSource).isTrue()
    }
}