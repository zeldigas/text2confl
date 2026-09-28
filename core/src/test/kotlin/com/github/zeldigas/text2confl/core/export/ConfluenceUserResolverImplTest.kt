package com.github.zeldigas.text2confl.core.export

import assertk.assertThat
import assertk.assertions.isNull
import com.github.zeldigas.confclient.ConfluenceAuthorizationException
import com.github.zeldigas.confclient.ConfluenceClient
import com.github.zeldigas.confclient.RequestDetails
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class ConfluenceUserResolverImplTest {

    @Test
    fun `Unresolved user when not authorized to read users`(@MockK client: ConfluenceClient) {
        coEvery { client.getUserByKey("acc-123") } throws ConfluenceAuthorizationException(
            RequestDetails("GET", "/rest/api/user"), 401, emptyMap(), "Unauthorized; scope does not match"
        )

        val resolver = ConfluenceUserResolverImpl(client)

        assertThat(resolver.resolve("acc-123")).isNull()
    }
}
