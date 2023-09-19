package com.github.zeldigas.text2confl.convert.markdown.diagram

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import com.github.zeldigas.text2confl.convert.markdown.diagram.KrokiDiagramsGenerator.Companion.SUPPORTED_DIAGRAMS
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.readText

@WireMockTest
class KrokiDiagramsGeneratorTest {

    @CsvSource(
        value = [
            "puml,plantuml",
            "plantuml,plantuml",
            "blockdiag,blockdiag"
        ]
    )
    @ParameterizedTest
    fun `Diagram generation`(
        inputLang: String,
        requestLang: String,
        @TempDir dir: Path,
        runtimeInfo: WireMockRuntimeInfo
    ) {
        stubFor(
            post("/").withRequestBody(
                WireMock.equalToJson(
                    """
            {"diagram_source":  "test", "diagram_type": "$requestLang", "output_format": "png", "diagram_options": {"hello":  "world"} }
        """.trimIndent()
                )
            ).willReturn(WireMock.ok().withBody("test response".toByteArray()).withHeader("content-type", "image/png"))
        )

        val generator = KrokiDiagramsGenerator(
            true, KrokiDiagramsGenerator.DEFAULT_FORMAT,
            URI.create(runtimeInfo.httpBaseUrl)
        )

        val result = generator.generate(
            "test", dir.resolve("test.png"),
            mapOf(
                DIAGRAM_FORMAT_ATTRIBUTE to inputLang,
                "option_hello" to "world"
            )
        )

        assertThat(result).isEqualTo(ImageInfo())
        assertThat(dir.resolve("test.png").readText()).isEqualTo("test response")
    }

    @Test
    fun `Kroki generator basic props`() {
        val generator =
            KrokiDiagramsGenerator(enabled = true, defaultFileFormat = "png", KrokiDiagramsGenerator.DEFAULT_SERVER)

        assertThat(generator.available()).isTrue()
        SUPPORTED_DIAGRAMS.forEach { assertThat(generator.supports(it)).isTrue() }
    }
}