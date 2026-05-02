---
labels: how-to
---

# Set a page title

text2confl determines the Confluence page title from the document itself. There are three methods, applied in priority
order — the first one that matches wins.

## Method 1: explicit `title` attribute

The `title` attribute sets the page title regardless of the document heading or filename.

**Markdown** (YAML front matter):

```markdown
---
title: My Page
---

Page content.
```

**AsciiDoc** (document attribute, overrides the document title line if both are present):

```asciidoc
= Document Heading
:title: My Page

Page content.
```

## Method 2: first-level heading / document title

If no `title` attribute is set, the first-level heading becomes the page title. The heading is removed from the rendered
output to avoid duplication.

**Markdown**:

```markdown
# My Page

Page content.
```

**AsciiDoc** (the document title line serves the same role):

```asciidoc
= My Page

Page content.
```

## Method 3: filename fallback

If neither a `title` attribute nor a first-level heading is present, the filename without its extension is used as the
page title.

A file named `my-page.md` with no heading becomes a page titled `my-page`.

## Notes

- All three methods produce the same result in Confluence — which one you use is a matter of preference. Pick one and
  apply it consistently across your docs.
- Page titles must be unique within a Confluence space. Duplicate titles cause an error before the upload starts.
- Changing a page title renames the page in Confluence. Cross-references within the managed docs tree are file-based and
  unaffected, but external Confluence pages linking to the old title will break.

## See also

- [Page attributes reference](../reference/page-attributes.md) — full syntax and all supported attributes
