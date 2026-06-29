package com.github.zeldigas.text2confl.convert.confluence

object LanguageMappers {
    val NOP: LanguageMapper = object : LanguageMapper {
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

interface LanguageMapper {

    fun mapToConfluenceLanguage(language: String): String?

    val supportedLanguages: Set<String>

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

val CLOUD_REMAPPING = mapOf(
    // PHP
    "php3" to "php",
    "php4" to "php",
    "php5" to "php",
// CSharp
    "cs" to "csharp" ,
    "c#" to "csharp",
// Python
    "py" to "python",
// JavaScript
    "js" to "javascript",
// C++
    "c++" to "cpp",
    "clike" to "cpp",
// Ruby
    "rb" to "ruby",
    "duby" to "ruby",
// Objective-C
    "objective-c" to "objectivec",
    "obj-c" to "objectivec",
    "objc" to "objectivec",
// TeX
    "latex" to "tex",
// Shell
    "bash" to "shell",
    "sh" to "shell",
    "ksh" to "shell",
    "zsh" to "shell",
// ActionScript
    "actionscript3" to "actionscript",
    "as" to "actionscript",
// JavaFX
    "jfx" to "javafx",
// VbNet
    "vb.net" to "vbnet",
    "vfp" to "vbnet",
    "clipper" to "vbnet",
    "xbase" to "vbnet",
// SQL
    "postgresql" to "sql",
    "postgres" to "sql",
    "plpgsql" to "sql",
    "psql" to "sql",
    "postgresql-console" to "sql",
    "postgres-console" to "sql",
    "tsql" to "sql",
    "t-sql" to "sql",
    "mysql" to "sql",
    "sqlite" to "sql",
// Perl
    "pl" to "perl",
// Pascal
    "pas" to "pascal",
    "objectpascal" to "pascal",
    "delphi" to "pascal",
// TypeScript
    "ts" to "typescript",
// CoffeeScript
    "coffee-script" to "coffeescript",
    "coffee" to "coffeescript",
// Haskell
    "hs" to "haskell",
// Erlang
    "erl" to "erlang",
// PowerShell
    "posh" to "powershell",
    "ps1" to "powershell",
    "psm1" to "powershell",
// Haxe
    "hx" to "haxe",
    "hxsl" to "haxe",
// Elixir
    "ex" to "elixir",
    "exs" to "elixir",
// Verilog
    "v" to "verilog",
// Sass
    "less" to "sass",
// reStructuredText
    "restructuredtext" to "rest",
    "rst" to "rest",
// QML
    "qbs" to "qml",
// FoxPro
    "foxpro" to "purebasic",
// Scheme
    "scm" to "scheme",
// CUDA
    "cu" to "cuda",
// Julia
    "jl" to "julia",
// Racket
    "rkt" to "racket",
// Ada
    "ada95" to "ada",
    "ada2005" to "ada",
// Mathematica
    "mma" to "mathematica",
    "nb" to "mathematica",
// StandardML
    "standardmL" to "sml",
    "standardml" to "sml",
// Objective-J
    "objective-j" to "objectivec",
    "objectivej" to "objectivec",
    "obj-j" to "objectivec",
    "objj" to "objectivec",
// Smalltalk
    "squeak" to "smalltalk",
    "st" to "smalltalk",
// Vala
    "vapi" to "vala",
// LiveScript
    "live-script" to "livescript",
// XQuery
    "xqy" to "xquery",
    "xq" to "xquery",
    "xql" to "xquery",
    "xqm" to "xquery",
// PlainText
    "plaintext" to "text",
// Yaml
    "yml" to "yaml",
// VisualBasic
    "vb" to "visualbasic",
// Dockerfile
    "docker" to "dockerfile",
// HCL
    "terraform" to "hcl",
// Protocol Buffers
    "proto" to "protobuf",
// Handlebars
    "mustache" to "handlebars",
// Gherkin
    "cucumber" to "gherkin",
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
    "abap", "actionscript", "ada",
    "applescript", "arduino", "autoit",
    "clojure", "coffeescript", "c",
    "cpp", "coldfusion", "csharp",
    "css", "cuda", "d", "dart",
    "diff", "dockerfile", "elixir",
    "erlang", "fortran", "gherkin",
    "go", "graphql", "groovy",
    "handlebars", "haskell", "haxe",
    "hcl", "html", "java", "javafx",
    "javascript", "json", "jsx",
    "julia", "kotlin",
    "lisp", "livescript", "lua",
    "markdown", "mathematica",
    "matlab", "nginx", "objectivec",
    "ocaml", "octave", "pascal",
    "perl", "php", "powershell",
    "prolog", "protobuf", "puppet",
    "purebasic", "python", "qml",
    "r", "racket", "rest", "ruby",
    "rust", "sass", "scala", "scheme",
    "shell", "smalltalk", "sml",
    "splunk-spl", "sql", "swift",
    "tcl", "tex", "text",
    "toml", "tsx", "typescript",
    "vala", "vbnet", "verilog",
    "vhdl", "visualbasic", "xml",
    "xml", "xml", "xquery",
    "yaml",
    )