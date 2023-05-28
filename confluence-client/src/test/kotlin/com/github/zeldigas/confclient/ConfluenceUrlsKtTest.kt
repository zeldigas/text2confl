package com.github.zeldigas.confclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.ktor.http.*
import org.junit.jupiter.api.Test

class ConfluenceUrlsKtTest {

    @Test
    fun `Link to api resource is generated`() {
        val result = makeLink("http://example.org/wiki", "/rest/api/content/att19988556/history")

        assertThat(result).isEqualTo(Url("http://example.org/wiki/rest/api/content/att19988556/history"))
    }

    @Test
    fun `Link to download resource is generated`() {
        val result = makeLink("http://example.org/wiki", "/download/attachments/19955792/mermaid-sample.png?version=1&modificationDate=1673204377836&cacheVersion=1&api=v2")

        assertThat(result).isEqualTo(Url("http://example.org/wiki/download/attachments/19955792/mermaid-sample.png?version=1&modificationDate=1673204377836&cacheVersion=1&api=v2"))
    }
}