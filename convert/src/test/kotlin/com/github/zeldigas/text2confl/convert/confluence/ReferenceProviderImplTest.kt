package com.github.zeldigas.text2confl.convert.confluence

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.github.zeldigas.text2confl.convert.PageHeader
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.div
import kotlin.text.ifEmpty

internal class ReferenceProviderImplTest {

    private val providerImpl = ReferenceProviderImpl(
        Path("docs"), mapOf(
            doc("docs/one.md", "Title One"),
            doc("docs/two.md", "Title Two"),
            doc("docs/sub/one.md", "Sub Title One"),
            doc("docs/sub/two.md", "Sub Title Two"),
            doc("docs/sub1/one.md", "Sub1 Title One"),
            doc("docs/sub/sub/a.md", "SubSub A"),
            doc("docs/sub/sub1/a.md", "SubSub1 A"),
            doc("docs/a spaced/a.md", "Spaced dir file"),
        )
    )

    private fun doc(path: String, title: String): Pair<Path, PageHeader> {
        return Path(path) to PageHeader(title, emptyMap())
    }

    @CsvSource(
        value = [
            "docs/one.md,./one.md,Title One",
            "docs/one.md,./two.md,Title Two",
            "docs/one.md,./two1.md,",
            "docs/one.md,one.md,Title One",
            "docs/one.md,sub/one.md,Sub Title One",
            "docs/one.md,sub/sub/../one.md,Sub Title One",
            "docs/one.md,sub/sub/a.md,SubSub A",
            "docs/one.md,a%20spaced/a.md,Spaced dir file",
            "docs/sub/two.md,one.md,Sub Title One",
            "docs/sub/two.md,./one.md,Sub Title One",
            "docs/sub/two.md,sub/one.md,",
            "docs/sub/two.md,sub/a.md,SubSub A",
            "docs/sub/two.md,../two.md,Title Two",
            "docs/sub/sub/a.md,../two.md,Sub Title Two",
            "docs/sub/sub/a.md,../sub1/a.md,SubSub1 A",
            "docs/sub/sub/a.md,../../sub1/one.md,Sub1 Title One",
            "docs/sub/sub/a.md,../sub1/one.md,",
            "docs/sub/sub/a.md,/absolute.png,",
        ]
    )
    @ParameterizedTest
    fun `Resolution of titles`(document: String, linkPath: String, expectedTitle: String?) {
        val result = providerImpl.resolveReference(Path(document), linkPath)

        assertThat(result).isEqualTo(expectedTitle?.ifEmpty { null }?.let { Xref(it, null) })
    }

    @Test
    fun `Absolute url to docs`() {
        val basePath = Path("docs").absolute()
        val provider = ReferenceProviderImpl(
            basePath, mapOf(
                doc("${basePath}/one.md", "Title One"),
                doc("${basePath}/two.md", "Title Two"),
                doc("${basePath}/sub/one.md", "Sub Title One"),
            )
        )

        val result = provider.resolveReference(basePath / "sub" / "doc.md", "../sub/one.md")

        assertThat(result).isEqualTo(Xref("Sub Title One", null))
    }

    @Test
    internal fun `Anchor resolution`() {
        val result = providerImpl.resolveReference(Path("docs/one.md"), "#test")

        assertThat(result).isNotNull().isEqualTo(Anchor("test"))
    }

    @Test
    internal fun `Xref with anchor resolution`() {
        val result = providerImpl.resolveReference(Path("docs/one.md"), "sub/one.md#test")

        assertThat(result).isNotNull().isEqualTo(Xref("Sub Title One", "test"))
    }

    @CsvSource(
        value = [
            "mailto:john@example.org",
            "http://example.org",
            "https://example.org",
            "https://example.org/docs/one.md",
            "file://example.org/docs/one.md", //for now this one is filtered out as well
            "ftp://example.org/docs/one.md"
        ]
    )
    @ParameterizedTest
    fun `Links of varios kinds are filtered out`(link:String) {
        val result = providerImpl.resolveReference(Path("docs/one.md"), link)

        assertThat(result).isNull()
    }
}