package com.github.zeldigas.text2confl.core.export

import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.text2confl.convert.markdown.export.ConfluenceUserResolver
import kotlinx.coroutines.runBlocking

class ConfluenceUserResolverImpl(private val client: ConfluenceClient) : ConfluenceUserResolver {

    private val cache: MutableMap<String, String?> = mutableMapOf()

    override fun resolve(userKey: String): String? {
        return cache.computeIfAbsent(userKey) { key ->
            runBlocking {
                val user = client.getUserByKey(key)
                user.username
            }
        }
    }

}