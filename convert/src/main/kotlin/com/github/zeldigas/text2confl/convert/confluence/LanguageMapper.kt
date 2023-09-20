package com.github.zeldigas.text2confl.convert.confluence

interface LanguageMapper {

    fun mapToConfluenceLanguage(language: String): String?

    val supportedLanguages: Set<String>

    companion object {
        fun nop(): LanguageMapper = object : LanguageMapper {
            override fun mapToConfluenceLanguage(language: String): String? = null
            override val supportedLanguages: Set<String>
                get() = emptySet()
        }

        fun forServer(defaultLanguage: String? = null, extraMapping: Map<String, String> = emptyMap()): LanguageMapper =
            LanguageMapperImpl(
                CONFLUENCE_SERVER_LANGUAGES,
                mapping = SERVER_REMAPPING + extraMapping,
                defaultLanguage = defaultLanguage
            )

        fun forCloud(defaultLanguage: String? = null, extraMapping: Map<String, String> = emptyMap()): LanguageMapper =
            LanguageMapperImpl(
                CONFLUENCE_CLOUD_LANGUAGES,
                mapping = CLOUD_REMAPPING + extraMapping,
                defaultLanguage = defaultLanguage
            )
    }

}

internal class LanguageMapperImpl(
    override val supportedLanguages: Set<String>,
    val defaultLanguage: String? = null,
    val mapping: Map<String, String> = LANG_REMAPPING
) : LanguageMapper {
    override fun mapToConfluenceLanguage(language: String): String? {
        val normalized = language.lowercase()
        return mapping.getOrDefault(normalized, normalized).takeIf { it in supportedLanguages } ?: defaultLanguage
    }
}

val LANG_REMAPPING = mapOf(
    "shell" to "bash",
    "zsh" to "bash",
    "sh" to "bash",
    "dockerfile" to "bash",
    "javascript" to "js"
)

val SERVER_REMAPPING = LANG_REMAPPING + mapOf(
    "yaml" to "yml",
    "html" to "xml",
)

val CLOUD_REMAPPING = LANG_REMAPPING + mapOf(
    "yml" to "yaml"
)

val CONFLUENCE_SERVER_LANGUAGES = setOf(
    "actionscript3", "applescript", "bash",
    "c#", "cpp", "css",
    "coldfusion", "delphi", "diff",
    "erl", "groovy", "xml",
    "java", "jfx", "js", "json",
    "php", "perl", "text",
    "powershell", "py", "ruby",
    "sql", "sass", "scala",
    "vb", "yml"
)

val CONFLUENCE_CLOUD_LANGUAGES = setOf(
    "abap", "actionscript3", "ada",
    "applescript", "arduino", "autoit",
    "bash", "c", "cpp", "clojure",
    "coffeescript", "coldfusion", "c#",
    "css", "cuda", "d",
    "dart", "diff", "elixir",
    "erl", "fortran", "foxpro",
    "go", "graphql", "groovy",
    "haskell", "haxe", "html",
    "java", "javafx", "js",
    "json", "jsx", "julia",
    "kotlin", "livescript", "lua",
    "mathematica", "matlab", "objective-c",
    "objective-j", "ocaml", "octave",
    "pas", "perl", "php",
    "text", "powershell", "prolog",
    "puppet", "py", "qbs",
    "r", "racket", "restructuredtext",
    "ruby", "rust", "sass",
    "scala", "scheme", "smalltalk",
    "splunk-spl", "sql", "standardlm",
    "swift", "tcl", "tex",
    "tsx", "typescript", "vala",
    "vbnet", "verilog", "vhdl",
    "vb", "xml", "xquery", "yaml"
)