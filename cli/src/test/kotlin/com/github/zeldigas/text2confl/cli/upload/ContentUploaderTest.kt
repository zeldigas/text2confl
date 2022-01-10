package com.github.zeldigas.text2confl.cli.upload

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isFailure
import com.github.zeldigas.text2confl.convert.Page
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class ContentUploaderTest(
    @MockK private val uploadOperations: PageUploadOperations
) {

    private val contentUploader = ContentUploader(uploadOperations)

    @BeforeEach
    internal fun setUp() {
        coEvery { uploadOperations.updatePageLabels(any(), any()) } just Runs
        coEvery { uploadOperations.updatePageAttachments(any(), any()) } just Runs
    }

    @Test
    internal fun `Upload pages hierarchy`() {
        val registry = mutableMapOf<Page, Long>()
        val fastPage = aPage(1, registry) {
            every { children } returns listOf(
                aPage(100, registry) {
                    every { children } returns emptyList()
                },
                aPage(2, registry) {
                    every { children } returns emptyList()
                },
                aPage(200, registry) {
                    every { children } returns emptyList()
                }
            )
        }
        val mediumPage = aPage(50, registry) {
            every { children } returns emptyList()
        }

        val slowPage = aPage(500, registry) {
            every { children } returns emptyList()
        }

        registry.forEach { (page, delayTime) ->
            coEvery { uploadOperations.createOrUpdatePageContent(page, "TEST", any()) } coAnswers {
                delay(delayTime)
                mockk() {
                    every { id } returns "$delayTime"
                }
            }
        }

        runBlocking { contentUploader.uploadPages(listOf(fastPage, mediumPage, slowPage), "TEST", "id") }

        registry.keys.forEach {
            coVerify { uploadOperations.createOrUpdatePageContent(it, "TEST", any()) }
        }
    }

    @Test
    internal fun `Failed upload abort others`() {
        val fastPage = mockk<Page>() {
            every { content } returns mockk()
            every { children } returns emptyList()
        }
        val failedPage = mockk<Page>()
        val slowPage = mockk<Page>()

        val fastServerPage = ServerPage("fastId", "id", emptyList(), emptyList())
        coEvery { uploadOperations.createOrUpdatePageContent(fastPage, "TEST", "id") } returns fastServerPage

        coEvery { uploadOperations.createOrUpdatePageContent(failedPage, any(), any()) } coAnswers {
            delay(100)
            throw RuntimeException("Upload failed")
        }

        val slowServerPage = ServerPage("slowId", "id", emptyList(), emptyList())
        coEvery { uploadOperations.createOrUpdatePageContent(slowPage, "TEST", "id") } coAnswers {
            delay(20000)
            slowServerPage
        }

        assertThat {
            runBlocking { contentUploader.uploadPages(listOf(slowPage, failedPage, fastPage), "TEST", "id") }
        }.isFailure().hasMessage("Upload failed")

        coVerify(exactly = 1) { uploadOperations.updatePageLabels(fastServerPage, any()) }
        coVerify(exactly = 0) { uploadOperations.updatePageLabels(slowServerPage, any()) }
    }

    private fun aPage(creationTime: Long, creationTimeRegistry: MutableMap<Page, Long>, block: Page.() -> Unit = {}) : Page {
        val page = mockk<Page>(block = {
            every { content } returns mockk()
            every { title } returns "Page created for ${creationTime}ms"
            block()
        })
        creationTimeRegistry[page] = creationTime
        return page
    }
}