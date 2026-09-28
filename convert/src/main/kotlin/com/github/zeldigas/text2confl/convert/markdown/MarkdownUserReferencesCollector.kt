package com.github.zeldigas.text2confl.convert.markdown

import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.NodeVisitor
import com.vladsch.flexmark.util.ast.VisitHandler

class MarkdownUserReferencesCollector {
    fun findUsers(ast: Document): MutableList<out String> {
        val users: MutableList<String> = mutableListOf()
        NodeVisitor(
            listOf(
                VisitHandler(ConfluenceUserNode::class.java) { users.add(it.text.toString()) },
            )
        ).visit(ast)
        return users
    }
}
