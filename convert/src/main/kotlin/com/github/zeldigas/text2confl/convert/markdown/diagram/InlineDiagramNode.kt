package com.github.zeldigas.text2confl.convert.markdown.diagram

import com.vladsch.flexmark.util.ast.Block
import com.vladsch.flexmark.util.sequence.BasedSequence

/**
 * Diagram code block replaced with content produced by [InlineDiagramMaker]. Content is rendered as is.
 */
class InlineDiagramNode(val content: String) : Block() {
    override fun getSegments(): Array<BasedSequence> {
        return EMPTY_SEGMENTS
    }
}
