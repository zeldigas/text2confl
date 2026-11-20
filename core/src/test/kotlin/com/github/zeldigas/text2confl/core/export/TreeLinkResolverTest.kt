package com.github.zeldigas.text2confl.core.export

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.zeldigas.text2confl.convert.markdown.export.ConfluenceLinksResolver
import org.junit.jupiter.api.Test
import java.nio.file.Path

class TreeLinkResolverTest {

    private val base = Path.of("out")
    private val pages = mapOf(
        TreeLinkResolver.Key("SP", "Root") to base.resolve("Root.md"),
        TreeLinkResolver.Key("SP", "Child Page") to base.resolve("Root/Child Page.md"),
        TreeLinkResolver.Key("SP", "Nested") to base.resolve("Root/Child Page/Nested.md"),
    )

    private fun resolver(dir: Path) = TreeLinkResolver(pages, "SP", dir, ConfluenceLinksResolver.NOP)

    @Test
    fun `Link to page in same directory`() {
        assertThat(resolver(base.resolve("Root")).resolve(null, "Child Page")).isEqualTo("Child%20Page.md")
    }

    @Test
    fun `Link to child and parent pages`() {
        assertThat(resolver(base).resolve("SP", "Nested")).isEqualTo("Root/Child%20Page/Nested.md")
        assertThat(resolver(base.resolve("Root/Child Page")).resolve(null, "Root")).isEqualTo("../../Root.md")
    }

    @Test
    fun `Pages outside of tree are resolved with fallback`() {
        assertThat(resolver(base).resolve(null, "Other")).isEqualTo("wiki://null/Other")
        assertThat(resolver(base).resolve("OTHER", "Root")).isEqualTo("wiki://OTHER/Root")
    }
}
