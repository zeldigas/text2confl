package com.github.zeldigas.text2confl.core.export

import com.github.zeldigas.text2confl.convert.markdown.export.ConfluenceLinksResolver
import java.nio.file.Path

/**
 * Resolves links to pages that are part of exported page tree as relative links to exported files.
 * Links to other pages are resolved with [fallback].
 */
class TreeLinkResolver(
    private val pages: Map<Key, Path>,
    private val currentSpace: String,
    private val sourceDir: Path,
    private val fallback: ConfluenceLinksResolver
) : ConfluenceLinksResolver {

    data class Key(val space: String, val title: String)

    override fun resolve(space: String?, title: String): String {
        val target = pages[Key(space ?: currentSpace, title)] ?: return fallback.resolve(space, title)
        return sourceDir.toAbsolutePath().normalize()
            .relativize(target.toAbsolutePath().normalize())
            .joinToString("/") { it.toString().replace(" ", "%20") }
    }
}
