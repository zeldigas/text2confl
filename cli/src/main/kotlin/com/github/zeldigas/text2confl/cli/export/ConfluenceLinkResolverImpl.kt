package com.github.zeldigas.text2confl.cli.export

import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.text2confl.convert.markdown.export.ConfluenceLinksResolver
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

class ConfluenceLinkResolverImpl(private val confluenceBaseUrl: Url, private val client: ConfluenceClient, val space: String?) : ConfluenceLinksResolver {

    private val cache: MutableMap<Key, String> = mutableMapOf()

    private data class Key(val space: String, val title: String)


    override fun resolve(space: String?, title: String): String {
        val realSpace = space ?: this.space ?: throw IllegalStateException("Space is not provided")
        return cache.computeIfAbsent(Key(realSpace, title)) { (sp, ttl) ->
            runBlocking {
                val links = client.getPage(sp, ttl, expansions = setOf("links")).links
                confluenceBaseUrl.toURI().resolve(links.getValue("webui")).toString()
            }
        }
    }
}