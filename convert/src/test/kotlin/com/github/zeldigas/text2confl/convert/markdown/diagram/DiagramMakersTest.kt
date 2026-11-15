package com.github.zeldigas.text2confl.convert.markdown.diagram

import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.messageContains
import assertk.assertions.prop
import com.github.zeldigas.text2confl.convert.markdown.DiagramMacroConfiguration
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Path

@ExtendWith(MockKExtension::class)
class DiagramMakersTest(
    @param:MockK val generator1: DiagramGenerator,
    @param:MockK val generator2: DiagramGenerator
) {

    val makers = DiagramMakersImpl(Path.of("."), listOf(generator1, generator2))

    @Test
    fun `First generator supporting lang is selected`() {
        every { generator1.supports("abc") } returns true
        every { generator2.supports("abc") } returns true

        val maker = makers.find("abc")

        assertThat(maker).isNotNull().isInstanceOf<ImageDiagramMaker>().all {
            prop(ImageDiagramMaker::baseDir).isEqualTo(Path.of("."))
            prop(ImageDiagramMaker::generator).isEqualTo(generator1)
        }
    }

    @Test
    fun `Null is returned when nothing is found`() {
        every { generator1.supports("abc") } returns false
        every { generator2.supports("abc") } returns false

        assertThat(makers.find("abc")).isNull()
    }

    @Test
    fun `Inline template is selected for any of its formats before generators`() {
        val inlineMakers = DiagramMakersImpl(
            Path.of("."), listOf(generator1),
            inlineMacros(
                mapOf(
                    "plantuml" to DiagramMacroConfiguration(
                        listOf("puml", "plantuml"), "{{lang}} {{type}}", mapOf("type" to "uml")
                    )
                )
            )
        )

        assertThat(inlineMakers.find("puml")).isNotNull().isInstanceOf<InlineDiagramMaker>()
            .transform { it.toDiagram("A -> B", emptyMap(), null) }
            .isEqualTo(GeneratedDiagram.InlineScript("puml uml"))
        assertThat(inlineMakers.find("plantuml")).isNotNull().isInstanceOf<InlineDiagramMaker>()
            .prop(InlineDiagramMaker::lang).isEqualTo("plantuml")
        verify(exactly = 0) { generator1.supports(any()) }
    }

    @Test
    fun `Format configured in several macros is rejected`() {
        assertFailure {
            inlineMacros(
                mapOf(
                    "first" to DiagramMacroConfiguration(listOf("puml", "plantuml"), "a"),
                    "second" to DiagramMacroConfiguration(listOf("plantuml"), "b"),
                )
            )
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Diagram format 'plantuml' is configured in both 'first' and 'second' macros")
    }

    @Test
    fun `Invalid template is rejected`() {
        assertFailure {
            inlineMacros(mapOf("broken" to DiagramMacroConfiguration(listOf("mermaid"), "{{#cdata}}{{source}}")))
        }.isInstanceOf<IllegalArgumentException>()
            .messageContains("Invalid template of diagram macro 'broken'")
    }

    @Test
    fun `Redefining automatic variables is rejected`() {
        assertFailure {
            inlineMacros(
                mapOf(
                    "mermaid" to DiagramMacroConfiguration(
                        listOf("mermaid"), "{{lang}}", mapOf("lang" to "x", "source" to "y", "tenant" to "z")
                    )
                )
            )
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Diagram macro 'mermaid' redefines reserved variables: 'lang', 'source'")
    }
}
