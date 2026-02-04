package com.github.zeldigas.text2confl.convert.markdown.ext

import com.vladsch.flexmark.ast.BlockQuote
import com.vladsch.flexmark.ast.Paragraph
import com.vladsch.flexmark.ext.admonition.AdmonitionBlock
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.parser.block.NodePostProcessor
import com.vladsch.flexmark.parser.block.NodePostProcessorFactory
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.ast.NodeTracker
import com.vladsch.flexmark.util.data.MutableDataHolder
import com.vladsch.flexmark.util.sequence.BasedSequence
import java.util.regex.Pattern

class GitHubAdmonitionExtension : Parser.ParserExtension {
    override fun parserOptions(options: MutableDataHolder?) {
        // No specific parser options needed
    }

    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.postProcessorFactory(object : NodePostProcessorFactory(false) {
            init {
                addNodes(BlockQuote::class.java)
            }

            override fun apply(document: Document): NodePostProcessor {
                return GhAdmonitionParser(document)
            }
        })
    }
}

internal class GhAdmonitionParser internal constructor(document: Document) : NodePostProcessor() {

    override fun process(state: NodeTracker, node: Node) {
        if (node !is BlockQuote) return

        val first = node.firstChild

        if (first !is Paragraph) return

        val matcher = ADMONITION_START.matcher(first.contentChars.trim())

        if (!matcher.matches()) return

        val admonitionType = mapGitHubTypeToAdmonitionType(matcher.group(1))

        val admonition = AdmonitionBlock()
        admonition.info = BasedSequence.of(admonitionType)
        node.children.drop(1).forEach {
            admonition.appendChild(it)
        }
        node.replaceWith(admonition, state)
    }

    private fun mapGitHubTypeToAdmonitionType(githubType: String): String {
        return when (val type = githubType.lowercase()) {
            "important" -> "info"
            "caution" -> "warning"
            else -> type
        }
    }


    companion object {
        private val ADMONITION_START: Pattern =
            Pattern.compile("^\\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)]\\s*$")
    }
}