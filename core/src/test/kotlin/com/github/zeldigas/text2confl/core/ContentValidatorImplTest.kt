package com.github.zeldigas.text2confl.core

import assertk.assertFailure
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.github.zeldigas.text2confl.convert.Validation
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.nio.file.Path

internal class ContentValidatorImplTest {

    val validator = ContentValidatorImpl()

    @Test
    internal fun `No issues in content`() {
        val autofix = false
        assertDoesNotThrow {
            validator.validate(listOf(mockk {
                every { content.validate(autofix) } returns Validation.Ok
                every { children } returns listOf(mockk {
                    every { content.validate(autofix) } returns Validation.Ok
                    every { children } returns emptyList()
                })
            }),autofix)
        }
    }

    @Test
    internal fun `Issues in content produce exception`() {
        val autofix = false
        assertFailure {
            validator.validate(
                listOf(
                    mockk {
                        every { content.validate(autofix) } returns Validation.Ok
                        every { children } returns listOf(mockk {
                            every { content.validate(autofix) } returns Validation.Invalid("err1")
                            every { source } returns Path.of("a", "b.txt")
                            every { children } returns emptyList()
                        })
                    },
                    mockk {
                        every { content.validate(autofix) } returns Validation.Invalid("err2")
                        every { source } returns Path.of("c.txt")
                        every { children } returns emptyList()
                    },

                    )
            ,autofix)
        }.isInstanceOf(ContentValidationFailedException::class)
            .transform { it.errors }.isEqualTo(listOf("${Path.of("a", "b.txt")}: err1", "c.txt: err2"))
    }
}