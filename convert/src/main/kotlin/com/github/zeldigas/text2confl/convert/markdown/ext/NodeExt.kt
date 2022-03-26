package com.github.zeldigas.text2confl.convert.markdown.ext

import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.ast.NodeTracker

fun Node.replaceWith(replacement:Node, nodeTracker: NodeTracker) {
    insertBefore(replacement)
    nodeTracker.nodeAdded(replacement)
    unlink()
    nodeTracker.nodeRemoved(this)
}