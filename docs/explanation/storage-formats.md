# Storage formats

text2confl supports two source formats: **Markdown** and **AsciiDoc**. You can use either format, or mix both formats within the same documentation tree.

## Why two formats?

The two formats exist for different reasons and serve different communities:

**Markdown** is the dominant format in software documentation today. It has a simple syntax, widespread tooling support (editors, linters, preview plugins), and is the default format on GitHub, GitLab, and most static site generators. If your team is already writing READMEs and wikis in Markdown, it requires no learning curve.

**AsciiDoc** is a more expressive format designed for technical documentation. It has native support for complex structures like cross-references, includes, callouts, admonitions, and fine-grained table control - features that require extensions or workarounds in Markdown. If your documentation has high complexity requirements (large multi-file documents, precise layout control, structured publishing), AsciiDoc is the better fit.

## Under the hood

text2confl uses a different library for each format:

- **Markdown** is parsed by [flexmark-java](https://github.com/vsch/flexmark-java), with several extensions enabled: tables, task lists, attributes, autolinks, typographic substitutions, and others. Confluence-specific features (admonitions, macros, status) are implemented as additional extensions on top.

- **AsciiDoc** is parsed by [AsciidoctorJ](https://github.com/asciidoctor/asciidoctorj), the Java wrapper around Asciidoctor. text2confl hooks into the conversion pipeline via custom Asciidoctor templates (written in Slim) that emit Confluence Storage Format XML instead of HTML.

The difference in approach matters: Markdown goes through flexmark's AST → custom visitor → CSF XML. AsciiDoc goes through Asciidoctor's full processing pipeline → custom backend templates → CSF XML. This means the two formats have slightly different extension points and some features are supported in one but not the other (e.g. file includes are native in AsciiDoc; Markdown does not have an equivalent).

## Choosing a format

| | Markdown | AsciiDoc |
|---|---|---|
| Syntax simplicity | Simpler | More verbose |
| Editor / tooling support | Excellent | Good |
| Cross-references between files | Via relative links | Native `xref:` |
| File includes | Not supported | Native `include::` |
| Table control | Basic | Fine-grained |
| Admonitions | Via custom extension | Native |
| Diagram support | PlantUML, Mermaid, Kroki | asciidoctor-diagram, Kroki |

Both formats support all core Confluence features: text formatting, tables, images, code blocks, diagrams, TOC, macros, page metadata, and attachments.

## Mixing formats

You can use both formats in the same documentation tree. text2confl detects the format by file extension (`.md` for Markdown, `.adoc` or `.asciidoc` for AsciiDoc) and applies the appropriate conversion pipeline per file.

## See also

- [Markdown reference](../reference/markdown.md) - supported Markdown syntax and features
- [AsciiDoc reference](../reference/asciidoc.adoc) - supported AsciiDoc syntax and features
