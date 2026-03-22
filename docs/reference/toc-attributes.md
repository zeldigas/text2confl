---
labels: reference
---

# Table of Contents attributes

You can control the representation of the table of contents
with [supported options](https://confluence.atlassian.com/doc/table-of-contents-macro-182682099.html):

| parameter | description                                                                                                                |
|-----------|----------------------------------------------------------------------------------------------------------------------------|
| type      | Style of TOC: `list` (default) or `flat`                                                                                   |
| maxLevel  | Maximum heading level to include in TOC. For example, if specified as `3`, headings 4, 5, and higher will not be included. |
| outline   | Adds numbering to TOC elements (1.1, 1.2, 1.2.3, and so on). Disabled by default; set to `true` to enable.                 |
| minLevel  | Minimum heading level to include in TOC. For example, if specified as `2`, heading 1 will not be included.                 |
| style     | Style of TOC items in list view: `circle`, `disc`, `square`, as well as other valid CSS styles.                            |
| separator | Separator between items for the ***flat*** TOC style: `brackets`, `braces`, `pipe`                                         |
| indent    | Indent for the ***list*** TOC style. Valid CSS size, e.g. `10px`                                                           |
| include   | Filter headings to include according to specified regex                                                                    |
| exclude   | Filter headings to exclude according to specified regex                                                                    |
| class     | Css class to add to wrapping div where toc is put                                                                          |

Example: a table of contents placed after a foreword section in a Markdown page:

```markdown
This page will tell you about important widget `FOO` usage in our project

[TOC maxLevel=2]

## Setting up

....

## Using in project

....

## Tips and tricks
```
