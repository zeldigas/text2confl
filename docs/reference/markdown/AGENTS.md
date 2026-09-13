# AGENTS.md



Guidance for working on files under `docs/reference/markdown/`.

## What these pages are

Each file is a per-feature Markdown syntax reference (`basic.md`, `tables.md`, `links-images.md`, `code.md`,
`diagrams.md`, `confluence-specific.md`), linked from the parent `docs/reference/markdown.md` overview page. They
double as live examples: text2confl publishes them, so the "rendered" half of every example is real Markdown that
gets converted for real, not a screenshot or hand-written description.

## Authoring pattern

Show source and rendered output side by side using a raw HTML `<table>` (HTML passes through Markdown untouched,
Confluence renders it fine):

```markdown
<table>
<thead>
<tr><th>Markdown</th><th>Confluence</th></tr>
</thead>
<tbody><tr>
<td>

​```markdown
**bold**
​```

</td><td>

**bold**

</td>
</tr></tbody></table>
```

The left cell is a fenced ```` ```markdown ```` block showing literal source; the right cell is the same content
written as live Markdown, so it actually renders. There's no include/snippet mechanism here (unlike the AsciiDoc
reference pages) — source and rendered copies are kept in sync by hand, so when you change one, update the other.

A plain Markdown table (`| Markdown | Confluence |`) is used instead of the HTML table for short one-line examples.

## Conventions

- Front matter on every page: `labels: supported-format,markdown,reference`.
- `[TOC]` (optionally `[TOC maxLevel=N]`) inserts a table of contents — see `../toc-attributes.md`.
- `_assets/` holds files referenced by examples (e.g. attachment demos in `links-images.md`) — not published as a
  standalone page because of the `_` prefix.
- Adding a new feature page: create the file, add front matter, then link it from `docs/reference/markdown.md`'s
  numbered feature list.
