package com.github.zeldigas.text2confl.core.export

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.zeldigas.confclient.ConfluenceClient
import io.ktor.http.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class ConfluenceLinkResolverImplTest {

    @Test
    fun `Cached resolution to mardown page`(@MockK client: ConfluenceClient) {
        every { client.confluenceBaseUrl } returns Url("http://example.org")
        coEvery { client.getPage("SP", "title") } returns mockk {
            every { links } returns mapOf("webui" to "/page/hello-world")
        }

        val resolver = ConfluenceLinkResolverImpl(client, "SP")

        val result = resolver.resolve(null, "title")
        val result1 = resolver.resolve(null, "title")

        assertThat(result).isEqualTo("http://example.org/page/hello-world")
        assertThat(result1).isEqualTo("http://example.org/page/hello-world")

        coVerify(exactly = 1) { client.getPage("SP", "title") }
    }
}