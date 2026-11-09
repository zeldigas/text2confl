package com.github.zeldigas.text2confl.core.upload.comments

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class InlineCommentsMergerTest {

    private val merger = InlineCommentsMerger()

    @Test
    fun `Real page sample from Confluence Cloud`() {
        val result = merger.merge(
            sample("configure-multi-tenancy.from-server-with-comments.html"),
            sample("configure-multi-tenancy.html")
        )

        assertThat(result.error).isNull()
        assertThat(CanonicalXml.canonical(result.body))
            .isEqualTo(CanonicalXml.canonical(sample("configure-multi-tenancy.expected.html")))
        assertThat(result.comments.map { it.ref to it.status }).containsExactly(
            "7a6153ad-05e1-46b9-b956-68f8c9b82776" to CommentStatus.KEPT,
            "9e95f5cb-99e9-4bdf-99e0-755d7ec789d5" to CommentStatus.KEPT,
            "aae132d9-2be3-4c9c-8f4a-380e71878cdd" to CommentStatus.KEPT,
            "d05551ef-3a2f-4695-a4a3-51f981f5f477" to CommentStatus.KEPT,
        )
    }

    @Test
    fun `Real sample with comments on external links reproduces Confluence markup`() {
        val result = merger.merge(sample("links.commented.html"), sample("links.html"))

        assertThat(result.error).isNull()
        assertThat(CanonicalXml.canonical(result.body.trim()))
            .isEqualTo(CanonicalXml.canonical(sample("links.commented.html").trim()))
        assertThat(result.comments.map { it.status }.distinct()).containsExactly(CommentStatus.KEPT)
        assertThat(result.comments.size).isEqualTo(6)
    }

    @Test
    fun `Server body right after upload has no markers, generated body is returned as is`() {
        val generated = sample("configure-multi-tenancy.html")

        val result = merger.merge(sample("configure-multi-tenancy.from-server.html"), generated)

        assertThat(result.body).isSameInstanceAs(generated)
        assertThat(result.comments).isEmpty()
    }

    @Test
    fun `Generated body is returned as is when no comment can be kept`() {
        val generated = "<p>Other   text&nbsp;here</p>"

        val result = merger.merge("""<p><ac:inline-comment-marker ac:ref="c1">Gone</ac:inline-comment-marker></p>""", generated)

        assertThat(result.body).isSameInstanceAs(generated)
        assertThat(result.lost.map { it.ref }).containsExactly("c1")
    }

    @Test
    fun `Malformed server body does not break merge`() {
        val generated = "<p>text</p>"

        val result = merger.merge("<p><ac:inline-comment-marker", generated)

        assertThat(result.body).isSameInstanceAs(generated)
        assertThat(result.comments).isEmpty()
    }

    @Test
    fun `Report contains old and new text`() {
        val result = merger.merge(
            """<p>This is <ac:inline-comment-marker ac:ref="c1">my old long text</ac:inline-comment-marker> in a paragraph.</p>""",
            "<p>This is my long text in a paragraph.</p>"
        )

        assertThat(result.comments).containsExactly(
            CommentOutcome("c1", "my old long text", "my long text", CommentStatus.ADJUSTED)
        )
        assertThat(result.adjusted.map { it.ref }).containsExactly("c1")
    }

    @ParameterizedTest
    @CsvSource(
        "1, 0.5",
        "2, 0.5",
        "6, 0.65",
        "10, 0.8",
        "50, 0.8",
    )
    fun `Pairing threshold depends on block length`(tokens: Int, expected: Double) {
        assertThat(BlockPairing.threshold(tokens)).isEqualTo(expected)
    }

    @Test
    fun `Text model flattens formatting, links and atoms`() {
        val root = StorageXml.parse(
            """<p>A <strong>bold</strong>
            |  <ac:link><ri:page ri:content-title="T" /><ac:plain-text-link-body><![CDATA[link]]></ac:plain-text-link-body></ac:link>&nbsp;and
            |<ac:structured-macro ac:name="status"><ac:parameter ac:name="title">X</ac:parameter></ac:structured-macro></p>""".trimMargin()
        )

        val block = collectBlocks(root).single()

        assertThat(block.norm).isEqualTo("A bold link and $OBJECT_CHAR")
    }

    @Test
    fun `Char transfer keeps offsets inside words`() {
        val transfer = SpanTransfer.transfer("abc defgh ij", "xx abc defgh ij", 5, 8)

        assertThat(transfer).isInstanceOf(Transfer.Kept::class)
        assertThat((transfer as Transfer.Kept).let { it.start to it.end }).isEqualTo(8 to 11)
    }

    @Test
    fun `Comment fragments split by formatting are joined`() {
        val root = StorageXml.parse(
            """<p><ac:inline-comment-marker ac:ref="c1">a </ac:inline-comment-marker><code><ac:inline-comment-marker ac:ref="c1">b</ac:inline-comment-marker>c</code> d</p>"""
        )
        val blocks = collectBlocks(root)

        val comments = collectComments(root, blocks)

        assertThat(comments.spans["c1"]?.map { it.text }).isNotNull().containsExactly("a b")
    }

    private fun sample(name: String) =
        javaClass.getResource("/inline-comments/sample/$name")!!.readText()
}
