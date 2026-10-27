package com.github.zeldigas.text2confl.core.upload

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.zeldigas.confclient.ConfluenceCloudClient
import com.github.zeldigas.confclient.PageAttachmentInput
import com.github.zeldigas.confclient.model.Attachment
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.io.path.Path

@ExtendWith(MockKExtension::class)
class DryRunCloudClientTest {

    @Test
    fun `User search is delegated to real client`(@MockK client: ConfluenceCloudClient) = runTest {
        val client = DryRunCloudClient(client)

        coEvery { client.findUserIdByEmail("value") } returns "result"

        assertThat(client.findUserIdByEmail("value")).isEqualTo("result")
    }

    @Test
    fun `Attachment update is not delegated to real client`(@MockK realClient: ConfluenceCloudClient) = runTest {
        val client = DryRunCloudClient(realClient)

        val result = client.updateAttachment(
            "123", "att456",
            PageAttachmentInput("diagram.svg", Path("diagram.svg"), 10L, "HASH:abc", "image/svg+xml")
        )

        assertThat(result).isEqualTo(Attachment("att456", "diagram.svg", emptyMap()))
        coVerify(exactly = 0) { realClient.updateAttachment(any(), any(), any()) }
    }

}
