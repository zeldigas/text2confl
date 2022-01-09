package com.github.zeldigas.kustantaja.convert.confluence

fun interface LanguageMapper {

    fun mapToConfluenceLanguage(language: String): String?

    companion object {
        fun nop() : LanguageMapper = LanguageMapper { null }
        fun forServer(defaultLanguage: String? = null): LanguageMapper = LanguageMapperImpl(CONFLUENCE_SERVER_LANGUAGES, defaultLanguage = defaultLanguage)
        fun forCloud(defaultLanguage:String? = null): LanguageMapper = LanguageMapperImpl(CONFLUENCE_CLOUD_LANGUAGES, defaultLanguage = defaultLanguage)
    }

}

private class LanguageMapperImpl(
    private val supportedLanguages: Set<String>,
    private val defaultLanguage: String? = null,
    private val mapping: Map<String, String> = LANG_REMAPPING
) : LanguageMapper {
    override fun mapToConfluenceLanguage(language: String): String? {
        val normalized = language.lowercase()
        return mapping.getOrDefault(normalized, normalized).takeIf { it in supportedLanguages } ?: defaultLanguage
    }
}

val LANG_REMAPPING = mapOf(
    "yaml" to "yml",
    "shell" to "bash",
    "zsh" to "bash",
    "sh" to "bash",
    "html" to "xml",
    "javascript" to "js"
)

private val CONFLUENCE_SERVER_LANGUAGES = setOf(
    "actionscript3", "applescript", "bash",
    "c#", "cpp", "css",
    "coldfusion", "delphi", "diff",
    "erl", "groovy", "xml",
    "java", "jfx", "js",
    "php", "perl", "text",
    "powershell", "py", "ruby",
    "sql", "sass", "scala",
    "vb", "yml"
)

private val CONFLUENCE_CLOUD_LANGUAGES = setOf(
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