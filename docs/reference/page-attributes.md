---
labels: reference
---

# Page attributes

Page attributes are key-value metadata attached to a document. text2confl reads them before conversion and uses them
to control page metadata in Confluence (title, labels, parent, properties, and more).

[TOC]

## Syntax

**Markdown** — YAML front matter block at the very beginning of the file, before any content:

```markdown
---
title: My page
labels: docs, team-a
---

Page content starts here.
```

**AsciiDoc** — document attributes using the `:key: value` syntax, in the document header (before the first blank
line). Attributes can appear before or after the document title, but the preferred placement is after it:

```asciidoc
= My page
:keywords: docs,team-a

Page content starts here.
```

Attribute values enclosed in `{}` or `[]` are treated as JSON, which allows setting complex page properties:

```markdown
---
property_nl_avisi_nh: { "isEnabled": true }
---
```

## Supported attributes

### Document title

Document title (name) can be defined in the following ways (from top priority to bottom):

1. `title` attribute
2. first-level heading at the top of document. If it is used, heading will be removed from resulting document to avoid
   duplication (unless the underlying format handles this automatically)
3. name of file

### Example 1 - title will be taken from *yaml front matter*

```markdown {title=my-page.md}
---
title: My page
---
Document content
```

Result title - `My page`

### Example 2 - title will be taken from *yaml front matter* regardless presence of top level header

```markdown {title=my-page.md}
---
title: My page
---

# Header that is not used as a title

Document content
```

Result title - `My page`

### Example 3 - title will be taken from first level header

```markdown {title=my-page.md}
# My Page

Document content
```

Result title - `My page`

**Note**: resulting page content will not include first level header

### Example 4 - title will be taken from filename

```markdown {title=my-page.md}
Document content
```

Resulting title - `my-page`

### Page labels

Confluence page labels can be set by the `labels` attribute (or `keywords` in AsciiDoc). The value can be a
comma-separated string or a list of strings (if the file format supports this).

Example - Markdown document that will have 3 labels (`one`, `two`, `three`):

```markdown
---
labels: one, two, three
---
Document content
```

### Custom parent

It is possible to specify a custom parent for any page using `parent` or `parentId`.

Attribute `parentId` should contain id of parent page and attribute `parent` should contain parent page title. When
both are specified, `parentId` takes precedence and `parent` is ignored.

Example - Markdown document with custom parent `Custom Parent Page`. Such document will be uploaded under this page if
it exists. When custom parent does not exist, upload procedure fails.

```markdown
---
parent: Custom Parent Page
---
Document content
```

### Page properties

Page properties are a set of special key-value pairs in Confluence. Confluence and plugins use them to configure page
behavior.

Properties can be of the following types: string, boolean, list of strings, object (json)

**text2confl** supports setting properties and extracts them from page attributes. 

Attribute patterns to be properties:

* Every attribute named `property_<property_name>` is treated as a page property.

Example:

Here we use `property_` prefix to define 2 properties - `first` is simple string and `complex` is an object with 2 fields

```yaml
property_first: hello
property_complex: { "first": one, "flag": false }
```

### Known properties:

* Confluence Cloud 
  * `editor` property holds the version of editor (v1 or v2)
  * `content-appearance-published` property set to `full-width` makes the page take the full screen width
* [appfire Numbered Heading plugin][page_numbering_plugin] [uses properties](https://appfire.atlassian.net/wiki/spaces/NH/pages/72680028/Page+properties)
  to enable page numbering on specific pages in Confluence Server.

### Limitations

Confluence Server does not support properties with dashes, trying to set such property will cause request to fail.

### Virtual pages

The `_virtual_` boolean attribute marks a page as a hierarchy placeholder. text2confl uses the file to determine the
page's position in the page tree but does not modify the page's content in Confluence.

```markdown
---
_virtual_: true
---
```

See [Virtual pages](../explanation/virtual-pages.md) for background and use cases.

[page_numbering_plugin]: https://appfire.atlassian.net/wiki/spaces/NH/overview