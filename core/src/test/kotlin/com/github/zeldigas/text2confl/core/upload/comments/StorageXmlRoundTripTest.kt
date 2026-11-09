package com.github.zeldigas.text2confl.core.upload.comments

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.toPath

/**
 * Parsing and serializing converter output must not change it: merged body is uploaded instead of generated one.
 * Uses golden outputs of converter tests and real page samples.
 */
internal class StorageXmlRoundTripTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("bodies")
    fun `Round trip keeps content`(name: String, file: Path) {
        val original = file.readText()

        val serialized = StorageXml.serialize(StorageXml.parse(original))

        assertThat(CanonicalXml.canonical(serialized)).isEqualTo(CanonicalXml.canonical(original))
    }

    companion object {
        @JvmStatic
        fun bodies(): List<Array<Any>> {
            val converterGolden = Path("../convert/src/test/resources/convert")
            val samples = StorageXmlRoundTripTest::class.java.getResource("/inline-comments/sample")!!.toURI().toPath()
            val files = listOf(converterGolden, samples)
                .filter { it.exists() }
                .flatMap { dir -> dir.listDirectoryEntries().filter { it.extension == "html" } }
                .sorted()
            assertThat(files).isNotEmpty()
            return files.map { arrayOf(it.name, it) }
        }
    }
}
