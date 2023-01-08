---
labels: supported-format,markdown
---

# Markdown

[TOC exclude="(.*\.md|.*\.kt)"]

# Document structure

Every Markdown document corresponds to separate confluence page.

Document can contain *YAML Front Matter* block at the very beginning - section in YAML format where various metadata can
be
defined like custom title for page or confluence labels. You read about supported attributes
on [dedicated page](../user-guide/page-attributes.md)

Example document with *yaml* block that has 2 *attributes*: `title` and `labels`

```markdown {title="Page with front matter"}
---
title: hello
labels: docs,intro
---

Document content 
```

# Supported features

On subpages, you will find details about various formatting aspects:

1. [](./markdown/basic.md) - about text styling, working with lists
2. [](./markdown/tables.md) - details of working with tables
3. [](./markdown/links-images.md) - using links (cross links and external), attaching images and other files
4. [](./markdown/code.md) -
5. [](./markdown/diagrams.md) - support for diagrmas defined as text
6. [](./markdown/confluence-specific.md) - details about confluence specific goodies, such as macros support (table of
   contents, status text, others)

How to read subpages:

1. Open rendered version of markdown file or uploaded page in Confluence (recommended)
2. Open raw markdown file side by side with rendered
3. When reading rendered page, find relevant part of raw page to understand how to write in Markdown

!!! note "A note about attriubtes"

    Multiple subpages refer to _attributes_ of code blocks, images and other elements. This is a
    custom [markdown extension](https://github.com/vsch/flexmark-java/wiki/Attributes-Extension) that helps to add
    additional information to elements in form of key-value pairs

