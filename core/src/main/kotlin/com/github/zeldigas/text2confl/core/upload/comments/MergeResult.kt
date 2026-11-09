package com.github.zeldigas.text2confl.core.upload.comments

/**
 * Result of merging inline comment markers from the server body into the generated body.
 *
 * @property body body to upload. It is exactly the generated body when no marker was inserted.
 * @property comments outcome for every comment (`ac:ref`) found in the server body, in document order.
 * @property error set when merge failed unexpectedly. In this case [body] is the unmodified generated body.
 */
data class InlineCommentsMergeResult(
    val body: String,
    val comments: List<CommentOutcome>,
    val error: Throwable? = null,
) {
    val kept: List<CommentOutcome>
        get() = comments.filter { it.status != CommentStatus.LOST }

    val lost: List<CommentOutcome>
        get() = comments.filter { it.status == CommentStatus.LOST || it.status == CommentStatus.PARTIAL }

    val adjusted: List<CommentOutcome>
        get() = comments.filter { it.status == CommentStatus.ADJUSTED }
}

/**
 * @property ref value of `ac:ref` attribute of the marker
 * @property oldText commented text in the server body (normalized whitespace, fragments joined with ` … `)
 * @property newText commented text in the result, when comment is kept
 * @property reason why comment (or part of it for [CommentStatus.PARTIAL]) was not kept
 */
data class CommentOutcome(
    val ref: String,
    val oldText: String,
    val newText: String?,
    val status: CommentStatus,
    val reason: LossReason? = null,
)

enum class CommentStatus {
    /** Marker is placed on exactly the same text */
    KEPT,

    /** Marker is placed on text that differs from the original one (commented text was edited) */
    ADJUSTED,

    /** Comment spans several blocks and only part of them are kept */
    PARTIAL,

    /** Comment is not present in the result */
    LOST
}

enum class LossReason {
    /** Block (paragraph, list item, etc.) containing comment is not found in new content */
    BLOCK_NOT_FOUND,

    /** Block is found, but commented text was changed too much */
    TEXT_CHANGED,

    /** Comment marker is located in a place that is not supported by merge */
    UNSUPPORTED_LOCATION
}
