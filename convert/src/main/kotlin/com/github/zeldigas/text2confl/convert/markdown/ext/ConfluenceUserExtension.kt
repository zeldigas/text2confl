package com.github.zeldigas.text2confl.convert.markdown

import com.vladsch.flexmark.parser.*
import com.vladsch.flexmark.parser.Parser.ParserExtension
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.data.MutableDataHolder
import com.vladsch.flexmark.util.sequence.BasedSequence

/**
 * Extension based on GfmUser extension but with userformat more suitable for confluence (dots and underscores allowed)
 */
class ConfluenceUserExtension : ParserExtension {
    override fun parserOptions(options: MutableDataHolder?) {
    }

    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.customInlineParserExtensionFactory(UserParserFactory());
    }
}

private class UserParserFactory : InlineParserExtensionFactory {
    override fun apply(inlineParser: LightInlineParser): InlineParserExtension {
        return UserTagParser()
    }

    override fun getAfterDependents(): MutableSet<Class<*>>? {
        return null
    }

    override fun getBeforeDependents(): MutableSet<Class<*>>? {
        return null
    }

    override fun affectsGlobalScope(): Boolean {
        return false
    }

    override fun getCharacters(): CharSequence {
        return "@"
    }
}

class ConfluenceUserNode(
    val openingMarker: BasedSequence,
    val text: BasedSequence
) : Node(spanningChars(openingMarker, text)) {
    override fun getSegments(): Array<BasedSequence> {
        return listOf(openingMarker, text).toTypedArray()
    }

    override fun getAstExtra(out: StringBuilder) {
        delimitedSegmentSpanChars(out, openingMarker, text, BasedSequence.NULL, "text")
    }
}

private class UserTagParser : InlineParserExtension {
    companion object {
        val PATTERN = """^(@)(("[^"]+")|([a-z\d_]([.a-z\d_-]+[a-z\d_])?))""".toRegex(RegexOption.IGNORE_CASE).toPattern()
    }

    override fun finalizeDocument(inlineParser: InlineParser) {
    }

    override fun finalizeBlock(inlineParser: InlineParser) {
    }

    override fun parse(inlineParser: LightInlineParser): Boolean {
        val index = inlineParser.index
        var isPossible = index == 0
        if (!isPossible) {
            val c = inlineParser.input[index - 1]
            if (!Character.isUnicodeIdentifierPart(c) && c != '-' && c != '.') {
                isPossible = true
            }
        }
        if (isPossible) {
            val matches = inlineParser.matchWithGroups(PATTERN) ?: return false;
            val openMarker = matches[1]!!
            val rawUsername = matches[2]!!
            val username = if (rawUsername.startsWith('"') && rawUsername.endsWith('"')) {
                rawUsername.trim('"')
            } else if (validUsername(rawUsername)) {
                rawUsername
            }else {
                null
            }
            if (username != null) {
                inlineParser.flushTextNode()
                val gitHubIssue = ConfluenceUserNode(openMarker, BasedSequence.of(username))
                inlineParser.block.appendChild(gitHubIssue)
                return true
            }
        }
        return false
    }

    private fun validUsername(basedSequence: BasedSequence): Boolean {
        return !basedSequence.startsWith('.') && !basedSequence.endsWith('.')
    }
}