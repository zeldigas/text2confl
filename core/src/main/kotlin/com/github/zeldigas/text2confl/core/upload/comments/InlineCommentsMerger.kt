package com.github.zeldigas.text2confl.core.upload.comments

/**
 * Carries inline comment markers (`ac:inline-comment-marker`) from the page body stored in Confluence into freshly
 * generated body.
 *
 * Generated body is always the base: server body is used only to find out where comments are. Markers are placed on
 * the same text in the generated body, even if blocks were moved, inserted or deleted around it, or the commented text
 * was slightly edited.
 *
 * See `docs/internal/inline-comments-design.md` for details.
 */
class InlineCommentsMerger {

    fun merge(serverBody: String, generatedBody: String): InlineCommentsMergeResult {
        if (!serverBody.contains(MARKER_TAG)) return InlineCommentsMergeResult(generatedBody, emptyList())
        return try {
            doMerge(serverBody, generatedBody)
        } catch (e: Exception) {
            InlineCommentsMergeResult(generatedBody, emptyList(), e)
        }
    }

    private fun doMerge(serverBody: String, generatedBody: String): InlineCommentsMergeResult {
        val serverRoot = StorageXml.parse(serverBody)
        val serverBlocks = collectBlocks(serverRoot)
        val comments = collectComments(serverRoot, serverBlocks)
        if (comments.refs.isEmpty()) return InlineCommentsMergeResult(generatedBody, emptyList())

        val generatedRoot = StorageXml.parse(generatedBody)
        val generatedBlocks = collectBlocks(generatedRoot)

        val commentedBlocks = comments.spans.values.flatten().map { it.block }.distinct()
        val pairs = BlockPairing(serverBlocks.size, generatedBlocks).pair(commentedBlocks)

        val insertions = mutableListOf<Insertion>()
        val outcomes = comments.refs.map { ref ->
            val spans = comments.spans[ref].orEmpty()
            if (spans.isEmpty()) {
                CommentOutcome(ref, "", null, CommentStatus.LOST, LossReason.UNSUPPORTED_LOCATION)
            } else {
                val results = spans.map { span -> transferSpan(span, pairs[span.block], generatedBlocks) }
                results.filterIsInstance<SpanResult.Kept>().forEach { insertions += it.insertion }
                outcome(ref, spans, results)
            }
        }

        if (insertions.isEmpty()) return InlineCommentsMergeResult(generatedBody, outcomes)

        insertions.distinctBy { listOf(it.ref, it.block, it.start, it.end) }
            .groupBy { it.block }
            .forEach { (block, blockInsertions) ->
                // Wrapping doesn't change text, so offsets stay valid in any order. Applying ranges from the start (longer
                // first on ties) nests overlapping comments the way Confluence does: the later comment goes inside the
                // earlier one in the overlap and continues after it.
                blockInsertions.sortedWith(compareBy<Insertion> { it.start }.thenByDescending { it.end })
                    .forEach {
                        val (rawStart, rawEnd) = block.toRaw(it.start, it.end)
                        MarkerInserter.wrap(block.content, rawStart, rawEnd, it.ref)
                    }
            }
        return InlineCommentsMergeResult(StorageXml.serialize(generatedRoot), outcomes)
    }

    private fun transferSpan(span: CommentSpan, pair: TextBlock?, generatedBlocks: List<TextBlock>): SpanResult {
        val lossReason = if (pair == null) {
            LossReason.BLOCK_NOT_FOUND
        } else {
            when (val transfer = SpanTransfer.transfer(span.block.norm, pair.norm, span.start, span.end)) {
                is Transfer.Kept -> return SpanResult.Kept(span, Insertion(span.ref, pair, transfer.start, transfer.end))
                is Transfer.Lost -> transfer.reason
            }
        }
        return AnchorSearch.find(span, generatedBlocks)?.let { SpanResult.Kept(span, it) }
            ?: SpanResult.Lost(span, lossReason)
    }

    private fun outcome(ref: String, spans: List<CommentSpan>, results: List<SpanResult>): CommentOutcome {
        val oldText = spans.joinToString(FRAGMENT_SEPARATOR) { it.text }
        val kept = results.filterIsInstance<SpanResult.Kept>()
        val lost = results.filterIsInstance<SpanResult.Lost>()
        if (kept.isEmpty()) {
            return CommentOutcome(ref, oldText, null, CommentStatus.LOST, lost.first().reason)
        }
        val newText = kept.joinToString(FRAGMENT_SEPARATOR) { it.insertion.text }
        val status = when {
            lost.isNotEmpty() -> CommentStatus.PARTIAL
            kept.all { it.insertion.text == it.span.text } -> CommentStatus.KEPT
            else -> CommentStatus.ADJUSTED
        }
        return CommentOutcome(ref, oldText, newText, status, lost.firstOrNull()?.reason)
    }

    private sealed interface SpanResult {
        val span: CommentSpan

        class Kept(override val span: CommentSpan, val insertion: Insertion) : SpanResult
        class Lost(override val span: CommentSpan, val reason: LossReason) : SpanResult
    }

    private companion object {
        const val FRAGMENT_SEPARATOR = " … "
    }
}

internal class Insertion(val ref: String, val block: TextBlock, val start: Int, val end: Int) {
    val text: String
        get() = block.norm.substring(start, end)
}

/**
 * Fallback for comments whose block was not paired or whose text changed in the paired block: looks for the exact
 * commented text, with a few words of context, anywhere in the generated content. The match is used only if it is
 * unique.
 */
internal object AnchorSearch {
    private const val CONTEXT_WORDS = 3
    private const val MIN_TEXT_WITHOUT_CONTEXT = 15

    fun find(span: CommentSpan, generated: List<TextBlock>): Insertion? {
        val text = span.text
        if (text.isBlank()) return null
        val before = span.block.norm.substring(0, span.start).split(' ').takeLast(CONTEXT_WORDS).joinToString(" ")
        val after = span.block.norm.substring(span.end).split(' ').take(CONTEXT_WORDS).joinToString(" ")

        val attempts = listOf(before to after, before to "", "" to after, "" to "")
            .distinct()
            .filter { (b, a) -> b.isNotEmpty() || a.isNotEmpty() || text.length >= MIN_TEXT_WITHOUT_CONTEXT }

        for ((b, a) in attempts) {
            val needle = b + text + a
            val matches = generated.flatMap { block -> occurrences(block.norm, needle).map { block to it } }
            if (matches.size == 1) {
                val (block, index) = matches.single()
                return Insertion(span.ref, block, index + b.length, index + b.length + text.length)
            }
        }
        return null
    }

    private fun occurrences(haystack: String, needle: String): List<Int> {
        val result = mutableListOf<Int>()
        var index = haystack.indexOf(needle)
        while (index >= 0) {
            result += index
            index = haystack.indexOf(needle, index + 1)
        }
        return result
    }
}
