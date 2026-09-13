# AGENTS.md



Guidance for working on files under `docs/reference/asciidoc/`.

## What these pages are

Each file is a per-feature AsciiDoc syntax reference (`basic.adoc`, `tables.adoc`, `links-images.adoc`, `code.adoc`,
`admonitions.adoc`, `toc.adoc`, `diagrams.adoc`, `includes.adoc`, `confluence-specific.adoc`), linked from the
parent `docs/reference/asciidoc.adoc` overview page. Like the Markdown reference set, these are live examples —
text2confl publishes them, so the "rendered" half of every example is real AsciiDoc that gets converted for real.

## Authoring pattern

Unlike the Markdown reference pages (which duplicate source and rendered content by hand), AsciiDoc's `include::`
directive with tagged regions keeps the two copies in sync automatically. Shared snippets live in
`_assets/example.adoc`, wrapped in tag markers:

```asciidoc
//tag::style-complex[]
Or mixed like *bold with _emphasis_ part*, all *_bold and italic_*
//end::style-complex[]
```

A reference page includes the same tagged region twice — once inside a `----` literal block to show the source,
once bare to render it live:

```asciidoc
[cols="a,a"]
|===
| AsciiDoc | Confluence

|
----
include::_assets/example.adoc[tag=style-complex]
----
|include::_assets/example.adoc[tag=style-complex]
|===
```

To add an example: add a new `//tag::name[]` / `//end::name[]` region to `_assets/example.adoc`, then reference
`tag=name` from the page — never duplicate example source inline.

`includes.adoc` documents the plain `include::` directive itself (whole-file includes, not tagged regions) — that's
a distinct feature being demonstrated, not the authoring pattern for this directory.

## Conventions

- Document attributes instead of YAML front matter: `:keywords:` (alias for `labels`), `:toc:`.
- `_assets/` holds the shared `example.adoc` snippet file plus other embedded assets (SVG logo, a `.puml` diagram
  source) — not published as a standalone page because of the `_` prefix.
- Adding a new feature page: create the file, add attributes, then link it from `docs/reference/asciidoc.adoc`'s
  numbered feature list.
