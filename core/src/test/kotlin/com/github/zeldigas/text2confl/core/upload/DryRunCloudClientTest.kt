package com.github.zeldigas.text2confl.core.upload

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.zeldigas.confclient.ConfluenceCloudClient
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class DryRunCloudClientTest {

    @Test
    fun `User search is delegated to real client`(@MockK client: ConfluenceCloudClient) = runTest {
        val client = DryRunCloudClient(client)

        coEvery { client.findUserIdByEmail("value") } returns "result"

        assertThat(client.findUserIdByEmail("value")).isEqualTo("result")
    }

}