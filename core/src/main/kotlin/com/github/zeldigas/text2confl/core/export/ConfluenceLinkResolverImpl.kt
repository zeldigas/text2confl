package com.github.zeldigas.text2confl.core.export

import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.confclient.makeLink
import com.github.zeldigas.text2confl.convert.markdown.export.ConfluenceLinksResolver
import kotlinx.coroutines.runBlocking

class ConfluenceLinkResolverImpl(private val client: ConfluenceClient, val space: String) : ConfluenceLinksResolver {

    private val cache: MutableMap<Key, String> = mutableMapOf()

    private data class Key(val space: String, val title: String)

    override fun resolve(space: String?, title: String): String {
        val realSpace = space ?: this.space
        return cache.computeIfAbsent(Key(realSpace, title)) { (sp, ttl) ->
            runBlocking {
                val links = client.getPage(sp, ttl).links
                makeLink(client.confluenceBaseUrl, links.getValue("webui")).toString()
            }
        }
    }
}