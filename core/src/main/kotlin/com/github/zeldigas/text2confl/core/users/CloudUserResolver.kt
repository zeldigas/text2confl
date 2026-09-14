package com.github.zeldigas.text2confl.core.users

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.zeldigas.confclient.ConfluenceUserSearchClient
import com.github.zeldigas.text2confl.convert.confluence.UserResolver
import com.sksamuel.aedile.core.asLoadingCache
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

private val MAPPER = jacksonObjectMapper()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

fun createFromStoredData(
    client: ConfluenceUserSearchClient,
    fileWithCache: Path
): CloudUserResolver {
    val data = fileWithCache.inputStream().use {
        MAPPER.readValue(it, ResolvedUsers::class.java)
    }
    return CloudUserResolver(client, data.users)
}

fun persistResolvedUsers(users: CloudUserResolver, destination: Path) {
    val usersData = runBlocking { users.cachedUsers() }
    destination.outputStream().use {
        MAPPER.writeValue(it, ResolvedUsers(usersData, Instant.now()))
    }
}

class CloudUserResolver(
    private val client: ConfluenceUserSearchClient,
    knownUsers: Map<String, String> = emptyMap()
) : UserResolver {

    private val cache = Caffeine.newBuilder()
        .asLoadingCache { email: String ->
            client.findUserIdByEmail(email)?.let { UserValue.Found(it) } ?: UserValue.Missing
        }

    init {
        knownUsers.forEach { (k, v) -> cache[k] = UserValue.Found(v) }
    }

    override suspend fun resolveUser(email: String): UserResolver.UserReference? {
        val result = cache.get(email)
        return if (result is UserValue.Found) user(result) else null
    }

    private fun user(result: UserValue.Found): UserResolver.UserReference = UserResolver.UserReference(
        UserResolver.UserIdFormat.ACCOUNT_ID, result.user
    )

    override suspend fun resolveUsers(users: List<String>): Map<String, UserResolver.UserReference> = coroutineScope {
        cache.getAll(users)
            .mapNotNull { (email, resolved) -> if (resolved is UserValue.Found) email to user(resolved) else null }
            .toMap()
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun registerReferencedUsers(email: List<String>) {
        GlobalScope.launch {
            resolveUsers(email)
        }
    }

    suspend fun cachedUsers(): Map<String, String> = cache.asMap()
        .filterValues { it is UserValue.Found }
        .map { (key, value) -> key to (value as UserValue.Found).user }
        .toMap()

    sealed class UserValue {
        object Missing : UserValue()
        data class Found(val user: String) : UserValue()
    }
}

data class ResolvedUsers(val users: Map<String, String>, val dateCreated: Instant)