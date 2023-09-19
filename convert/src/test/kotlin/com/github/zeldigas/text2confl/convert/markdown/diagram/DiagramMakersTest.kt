package com.github.zeldigas.text2confl.convert.markdown.diagram

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Path

@ExtendWith(MockKExtension::class)
class DiagramMakersTest(
    @MockK val generator1: DiagramGenerator,
    @MockK val generator2: DiagramGenerator
) {

    val makers = DiagramMakersImpl(Path.of("."), listOf(generator1, generator2))

    @Test
    fun `First generator supporting lang is selected`() {
        every { generator1.supports("abc") } returns true
        every { generator2.supports("abc") } returns true

        val maker = makers.find("abc")

        assertThat(maker).isNotNull().all {
            prop(DiagramMaker::baseDir).isEqualTo(Path.of("."))
            prop(DiagramMaker::generator).isEqualTo(generator1)
        }
    }

    @Test
    fun `Null is returned when nothing is found`() {
        every { generator1.supports("abc") } returns false
        every { generator2.supports("abc") } returns false

        assertThat(makers.find("abc")).isNull()
    }
}