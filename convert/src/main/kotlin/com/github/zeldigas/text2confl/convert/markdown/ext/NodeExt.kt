package com.github.zeldigas.text2confl.convert.markdown.ext

import com.vladsch.flexmark.ext.attributes.AttributeNode
import com.vladsch.flexmark.ext.attributes.internal.NodeAttributeRepository
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.ast.NodeTracker

fun Node.replaceWith(replacement: Node, nodeTracker: NodeTracker) {
    insertBefore(replacement)
    nodeTracker.nodeAdded(replacement)
    unlink()
    nodeTracker.nodeRemoved(this)
}

interface AttributeRepositoryAware {
    val nodeAttributeRepository: NodeAttributeRepository

    val Node.attributesMap: Map<String, String>
        get() = (nodeAttributeRepository[this]?.flatMap { it.children.filterIsInstance<AttributeNode>() }
            ?: emptyList()).associate { it.name.unescape() to it.value.unescape() }
}