package com.github.zeldigas.text2confl.core.users

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.github.zeldigas.confclient.ConfluenceUserSearchClient
import com.github.zeldigas.text2confl.convert.confluence.UserResolver
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class CloudUserResolverTest(
    @MockK private val client: ConfluenceUserSearchClient
) {

    @Test
    fun `resolveUser returns account id when user found`() = runTest {
        val resolver = CloudUserResolver(client)
        coEvery { client.findUserIdByEmail("user@example.com") } returns "acc-123"

        assertThat(resolver.resolveUser("user@example.com")).isEqualTo(UserResolver.UserReference(UserResolver.UserIdFormat.ACCOUNT_ID, "acc-123"))
    }

    @Test
    fun `resolveUser returns null when user not found`() = runTest {
        val resolver = CloudUserResolver(client)
        coEvery { client.findUserIdByEmail("missing@example.com") } returns null

        assertThat(resolver.resolveUser("missing@example.com")).isNull()
    }

    @Test
    fun `resolveUser caches result and calls client only once for repeated lookups`() = runTest {
        val resolver = CloudUserResolver(client)
        coEvery { client.findUserIdByEmail("user@example.com") } returns "acc-123"

        resolver.resolveUser("user@example.com")
        resolver.resolveUser("user@example.com")

        coVerify(exactly = 1) { client.findUserIdByEmail("user@example.com") }
    }

    @Test
    fun `resolveUsers returns map of found users only`() = runTest {
        val resolver = CloudUserResolver(client)
        coEvery { client.findUserIdByEmail("a@example.com") } returns "acc-a"
        coEvery { client.findUserIdByEmail("b@example.com") } returns null
        coEvery { client.findUserIdByEmail("c@example.com") } returns "acc-c"

        val result = resolver.resolveUsers(listOf("a@example.com", "b@example.com", "c@example.com"))

        assertThat(result).isEqualTo(mapOf(
            "a@example.com" to UserResolver.UserReference(UserResolver.UserIdFormat.ACCOUNT_ID, "acc-a"),
            "c@example.com" to UserResolver.UserReference(UserResolver.UserIdFormat.ACCOUNT_ID, "acc-c")
        ))
    }

    @Test
    fun `concurrent resolveUser calls for same email trigger only one client request`() = runTest {
        val resolver = CloudUserResolver(client)
        coEvery { client.findUserIdByEmail("user@example.com") } returns "acc-123"

        val results = (1..5).map { async { resolver.resolveUser("user@example.com") } }.awaitAll()

        assertThat(results.distinct()).isEqualTo(listOf(UserResolver.UserReference(UserResolver.UserIdFormat.ACCOUNT_ID, "acc-123")))
        coVerify(exactly = 1) { client.findUserIdByEmail("user@example.com") }
    }

    @Test
    fun `known users are returned without calling client`() = runTest {
        val resolver = CloudUserResolver(client, knownUsers = mapOf("known@example.com" to "acc-known"))

        assertThat(resolver.resolveUser("known@example.com")).isEqualTo(UserResolver.UserReference(UserResolver.UserIdFormat.ACCOUNT_ID, "acc-known"))

        coVerify(exactly = 0) { client.findUserIdByEmail(any()) }
    }

    @Test
    fun `cachedUsers returns pre-populated known users`() = runTest {
        val resolver = CloudUserResolver(client, knownUsers = mapOf("a@example.com" to "acc-a", "b@example.com" to "acc-b"))

        assertThat(resolver.cachedUsers()).isEqualTo(mapOf("a@example.com" to "acc-a", "b@example.com" to "acc-b"))
    }

    @Test
    fun `cachedUsers includes users resolved via client`() = runTest {
        val resolver = CloudUserResolver(client)
        coEvery { client.findUserIdByEmail("user@example.com") } returns "acc-123"

        resolver.resolveUser("user@example.com")

        assertThat(resolver.cachedUsers()).isEqualTo(mapOf("user@example.com" to "acc-123"))
    }

    @Test
    fun `cachedUsers does not include users not found by client`() = runTest {
        val resolver = CloudUserResolver(client)
        coEvery { client.findUserIdByEmail("missing@example.com") } returns null

        resolver.resolveUser("missing@example.com")

        assertThat(resolver.cachedUsers()).isEqualTo(emptyMap())
    }
}
