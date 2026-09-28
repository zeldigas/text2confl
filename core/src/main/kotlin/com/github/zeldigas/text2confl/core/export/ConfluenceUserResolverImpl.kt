package com.github.zeldigas.text2confl.core.export

import com.github.zeldigas.confclient.ConfluenceAuthorizationException
import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.text2confl.convert.markdown.export.ConfluenceUserResolver
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking

class ConfluenceUserResolverImpl(private val client: ConfluenceClient) : ConfluenceUserResolver {

    private val cache: MutableMap<String, String?> = mutableMapOf()

    override fun resolve(userKey: String): String? {
        return cache.computeIfAbsent(userKey) { key ->
            runBlocking {
                try {
                    val user = client.getUserByKey(key)
                    user.username ?: user.email
                } catch (e: ConfluenceAuthorizationException) {
                    logger.warn(e) { "Not enough permissions to resolve user by key" }
                    null
                }
            }
        }
    }

    private companion object {
        val logger = KotlinLogging.logger {  }
    }

}