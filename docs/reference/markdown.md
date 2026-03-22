---
labels: supported-format,markdown
---

# Markdown

[TOC exclude="(.*\.md|.*\.kt)"]

# Document structure

Every Markdown document corresponds to separate Confluence page.

Document can contain *YAML Front Matter* block at the very beginning - section in YAML format where various metadata can
be defined like custom title for page, page labels and properties. You can read about supported attributes
on [dedicated page](./page-attributes.md).

If attribute value is enclosed in `{}` or in `[]`, then value is treated as JSON and parsed. This approach allows to set
complex properties on page.

Example document with *yaml* block that has 4 *attributes*: `title`, `labels` and 2 properties - `simple` and `nl_avisi_nh`

```markdown {title="Page with front matter"}
---
title: hello
labels: docs,intro
property_simple: custom_value
property_nl_avisi_nh: { "isEnabled": true }
---

Document content 
```

# Supported features

On subpages, you will find details about various formatting aspects:

1. [](./markdown/basic.md) - about text styling, working with lists
2. [](./markdown/tables.md) - details of working with tables
3. [](./markdown/links-images.md) - using links (cross links and external), attaching images and other files
4. [](./markdown/code.md) - code inline and blocks
5. [](./markdown/diagrams.md) - support for diagrams defined as text
6. [](./markdown/confluence-specific.md) - details about Confluence specific features: macros, status, admonitions

!!! note "A note about attributes"

    Multiple subpages refer to _attributes_ of code blocks, images and other elements. This is a
    custom [markdown extension](https://github.com/vsch/flexmark-java/wiki/Attributes-Extension) that helps to add
    additional information to elements in form of key-value pairs

