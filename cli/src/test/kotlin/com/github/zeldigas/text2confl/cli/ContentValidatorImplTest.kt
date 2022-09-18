package com.github.zeldigas.text2confl.cli

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isSuccess
import com.github.zeldigas.text2confl.convert.Validation
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.nio.file.Path

internal class ContentValidatorImplTest {

    val validator = ContentValidatorImpl()

    @Test
    internal fun `No issues in content`() {
        assertThat {
            validator.validate(listOf(mockk {
                every { content.validate() } returns Validation.Ok
                every { children } returns listOf(mockk {
                    every { content.validate() } returns Validation.Ok
                    every { children } returns emptyList()
                })
            }))
        }.isSuccess()
    }

    @Test
    internal fun `Issues in content produce exception`() {
        assertThat {
            validator.validate(
                listOf(
                    mockk {
                        every { content.validate() } returns Validation.Ok
                        every { children } returns listOf(mockk {
                            every { content.validate() } returns Validation.Invalid("err1")
                            every { source } returns Path.of("a", "b.txt")
                            every { children } returns emptyList()
                        })
                    },
                    mockk {
                        every { content.validate() } returns Validation.Invalid("err2")
                        every { source } returns Path.of("c.txt")
                        every { children } returns emptyList()
                    },

                    )
            )
        }.isFailure().isInstanceOf(ContentValidationFailedException::class)
            .transform { it.errors }.isEqualTo(listOf("a/b.txt: err1", "c.txt: err2"))
    }
}