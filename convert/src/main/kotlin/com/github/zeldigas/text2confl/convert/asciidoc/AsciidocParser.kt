package com.github.zeldigas.text2confl.convert.asciidoc

import com.github.zeldigas.text2confl.convert.confluence.LanguageMapper
import com.vladsch.flexmark.util.sequence.Escaping.unescapeHtml
import org.asciidoctor.*
import org.asciidoctor.ast.Document
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path


class AsciidocParser(
    private val config: AsciidoctorConfiguration
) {

    companion object {
        private val TEMPLATES_LOCATION = "/com/github/zeldigas/text2confl/asciidoc"
    }

    private val ADOC: Asciidoctor = Asciidoctor.Factory.create().also {asciidoc ->
        config.libsToLoad.forEach { asciidoc.requireLibrary(it)}
        if (config.loadBundledMacros) {
            DefaultMacros().register(asciidoc)
        }
    }

    private val templatesLocation: Path by lazy {
        val templateResources = AsciidocParser::class.java.getResource(TEMPLATES_LOCATION)!!.toURI()
        if (templateResources.scheme == "file") {
            Path(templateResources.path)
        } else {
            val dest = Files.createTempDirectory("asciidoc_templates").toAbsolutePath()

            extractTemplatesTo(dest, templateResources)

            dest
        }
    }

    fun parseDocumentHeader(file: Path): Document {
        return ADOC.loadFile(file.toFile(), headerParsingOptions())
    }

    private fun headerParsingOptions() = parserOptions { }

    fun parseDocument(file: Path, parameters: AsciidocRenderingParameters): Document {
        return ADOC.loadFile(file.toFile(), htmlConversionOptions(createAttributes(parameters)))
    }

    fun parseDocument(source: String, parameters: AsciidocRenderingParameters): Document {
        return ADOC.load(source, htmlConversionOptions(createAttributes(parameters)))
    }

    private fun createAttributes(parameters: AsciidocRenderingParameters) = mapOf(
        "t2c-language-mapper" to parameters.languageMapper,
        "t2c-ref-provider" to parameters.referenceProvider,
        "t2c-auto-text" to parameters.autoText,
        "t2c-add-auto-text" to parameters.includeAutoText,
        "t2c-attachments-collector" to parameters.attachmentsCollector,
        "t2c-space" to parameters.space,
        "t2c-decoder" to Converter,
        "idseparator" to "-",
        "idprefix" to ""
    ) + config.attributes + parameters.extraAttrs

    private fun htmlConversionOptions(attrs: Map<String, Any?>) = parserOptions {
        attributes(
            Attributes.builder().attributes(attrs)
                .sourceHighlighter("none")
                .build()
        )
        templateDirs(templatesLocation.toFile())
    }

    private fun parserOptions(configurer: OptionsBuilder.() -> Unit) = Options.builder()
        .safe(SafeMode.UNSAFE)
        .backend("xhtml5")
        .also(configurer)
        .build()

}

data class AsciidocRenderingParameters(
    val languageMapper: LanguageMapper,
    val referenceProvider: AsciidocReferenceProvider,
    val autoText: String,
    val includeAutoText: Boolean,
    val space: String,
    val attachmentsCollector: AsciidocAttachmentCollector,
    val extraAttrs: Map<String, Any?> = emptyMap()
)

object Converter {
    fun convert(string: String): String = unescapeHtml(string)
}