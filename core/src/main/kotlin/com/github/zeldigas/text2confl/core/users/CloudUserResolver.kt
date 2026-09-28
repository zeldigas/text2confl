package com.github.zeldigas.text2confl.core.users

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.zeldigas.confclient.ConfluenceAuthorizationException
import com.github.zeldigas.confclient.ConfluenceUserSearchClient
import com.github.zeldigas.text2confl.convert.confluence.UserResolver
import com.sksamuel.aedile.core.asLoadingCache
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.*

private val MAPPER = jacksonObjectMapper()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .registerModule(JavaTimeModule())

fun createFromStoredData(
    client: ConfluenceUserSearchClient,
    fileWithCache: Path
): CloudUserResolver {
    if (fileWithCache.exists()) {
        val data = loadUsersOrEmpty(fileWithCache)
        return CloudUserResolver(client, data?.users ?: emptyMap())
    } else {
        return CloudUserResolver(client)
    }
}

fun persistResolvedUsers(users: CloudUserResolver, destination: Path) {
    val usersData = runBlocking { users.cachedUsers() }
    if (!destination.parent.exists()) {
        destination.parent.createDirectories()
    }
    val storedUsers = loadUsersOrEmpty(destination)
    if (storedUsers != null && storedUsers.users == usersData) return
    destination.outputStream().use {
        MAPPER.writeValue(it, ResolvedUsers(usersData, Instant.now()))
    }
}

private fun loadUsersOrEmpty(fileWithCache: Path): ResolvedUsers? {
    return try {
        fileWithCache.inputStream().use {
            MAPPER.readValue(it, ResolvedUsers::class.java)
        }
    } catch (_: Exception) {
        fileWithCache.deleteExisting()
        null
    }
}

class CloudUserResolver(
    private val client: ConfluenceUserSearchClient,
    knownUsers: Map<String, String> = emptyMap()
) : UserResolver {

    private val cache = Caffeine.newBuilder()
        .asLoadingCache { email: String ->
            try {
                client.findUserIdByEmail(email)?.let { UserValue.Found(it) } ?: UserValue.Missing
            } catch (e: ConfluenceAuthorizationException) {
                logger.warn(e) { "Not enough permissions to resolve user: $email" }
                UserValue.Missing
            }
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

    private companion object {
        val logger = KotlinLogging.logger { }
    }
}

data class ResolvedUsers(val users: Map<String, String>, val dateCreated: Instant)