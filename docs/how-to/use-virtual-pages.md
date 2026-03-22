# Use virtual pages for sparse page trees

This guide shows how to use virtual pages to organize your files when you don't want to manage all Confluence pages as code, or when your managed pages are scattered across multiple locations in the space hierarchy.

For conceptual background, see [Virtual pages](../explanation/virtual-pages.md).

## When to use virtual pages

Use a virtual page as a placeholder when:

- A parent page exists in Confluence but you don't want text2confl to manage its content (e.g. it uses complex formatting like multi-column layouts or custom macros)
- You want your file tree to mirror the Confluence page hierarchy without fully managing every intermediate page

## Create a virtual page

Create a Markdown file for the parent page and set the `_virtual_` attribute to `true` in the YAML front matter:

```markdown
---
title: Parent page
_virtual_: true
---
```

Or in AsciiDoc:

```asciidoc
:title: Parent page
:_virtual_: true
```

text2confl will use this file to determine the page's position in the hierarchy, but will not modify the page's content in Confluence.

## Example: sparse page tree

Suppose you have managed pages under `Parent page` and `Another page`, but `Parent page` already exists in Confluence with custom formatting you want to keep:

```text
├── another-page.md
├── parent-page.md         ← virtual (placeholder only)
├── parent-page/
│   ├── page-a.md          ← managed
│   └── page-b.md          ← managed
```

`parent-page.md`:

```markdown
---
title: Parent page
_virtual_: true
---
```

text2confl will place `page-a.md` and `page-b.md` as children of `Parent page` without touching `Parent page`'s content.

## Important constraints

- Virtual pages are never associated with a tenant in multi-tenant setups - they can coexist across teams.
- Virtual pages are excluded from orphan cleanup.

## See also

- [Virtual pages](../explanation/virtual-pages.md) - conceptual explanation
- [Page attributes](../reference/page-attributes.md) - full list of supported page attributes including `_virtual_`
