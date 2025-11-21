package com.github.zeldigas.confclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.ktor.http.*
import org.junit.jupiter.api.Test

class ConfluenceUrlsKtTest {

    @Test
    fun `Link to api resource is generated`() {
        val result = makeLink("http://example.org/wiki", "/rest/api/content/att19988556/history", rootApiLink = false)

        assertThat(result).isEqualTo(Url("http://example.org/wiki/rest/api/content/att19988556/history"))
    }

    @Test
    fun `Link to download resource is generated`() {
        val result = makeLink(
            "http://example.org/wiki",
            "/download/attachments/19955792/mermaid-sample.png?version=1&modificationDate=1673204377836&cacheVersion=1&api=v2",
            rootApiLink = false
        )

        assertThat(result).isEqualTo(Url("http://example.org/wiki/download/attachments/19955792/mermaid-sample.png?version=1&modificationDate=1673204377836&cacheVersion=1&api=v2"))
    }

    @Test
    fun `Link from repo root is generated`() {
        val result = makeLink(
            "http://example.atlassian.net/wiki",
            "/wiki/api/v2/pages/851969/direct-children?limit=2&cursor=eyJpZCI6IjE1NDAwOTciLCJjb250ZW50T3JkZXIiOiJjaGlsZC1wb3NpdGlvbiIsImNvbnRlbnRPcmRlclZhbHVlIjoxMTIzNDk1MzF9",
            rootApiLink = true
        )

        assertThat(result).isEqualTo(Url("http://example.atlassian.net/wiki/api/v2/pages/851969/direct-children?limit=2&cursor=eyJpZCI6IjE1NDAwOTciLCJjb250ZW50T3JkZXIiOiJjaGlsZC1wb3NpdGlvbiIsImNvbnRlbnRPcmRlclZhbHVlIjoxMTIzNDk1MzF9"))
    }
}