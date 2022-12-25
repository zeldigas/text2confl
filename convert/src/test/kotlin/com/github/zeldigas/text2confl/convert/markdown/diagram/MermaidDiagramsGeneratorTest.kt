package com.github.zeldigas.text2confl.convert.markdown.diagram

import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class MermaidDiagramsGeneratorTest {

    @Test
    fun mermaidCommandInvocation() {
        val generator = MermaidDiagramsGenerator(defaultFormat = "png", command = "/home/dmitry/.npm-packages/bin/mmdc")
        val script = """
            graph LR;
              A-->B;
        """.trimIndent()
        generator.generate(script, Path("/tmp/t2c_mmdc1.png"))
    }
}