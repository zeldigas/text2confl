# Table of Contents attributes

You can control representation of table of contents
with [supported options](https://confluence.atlassian.com/doc/table-of-contents-macro-182682099.html):

| parameter | description                                                                                                    |
|-----------|----------------------------------------------------------------------------------------------------------------|
| type      | Style of TOC: `list` (default) or `flat`                                                                       |
| maxLevel  | maximum heading level to include in toc. E.g. if specified as `3` headings 4, 5 and so on will not be included |
| outline   | Adds numbering to TOC elements (1.1, 1.2, 1.2.3 and so on). By default is disabled, to enable set to `true`    |
| minLevel  | minimum heading level to include in toc. E.g. if specified as `2` headings 1 will not be included              |
| style     | style of toc items in list: `circle`, `disc`, `square` as well as other valid css style of elements            |
| separator | Separator between items for ***flat*** toc style: `brackets`, `braces`, `pipe`                                 |
| indent    | Indent for a ***list*** toc style. Valid css size, e.g. `10px`                                                 |
| include   | Filter headings to include according to specified regex                                                        |
| exclude   | Filter headings to exclude according to specified regex                                                        |
| class     | Css class to add to wrapping div where toc is put                                                              |

Example: table of contents that is put after foreword block on markdown page:

```markdown
This page will tell you about important widget `FOO` usage in our project

[TOC maxLevel=2]

## Setting up

....

## Using in project

....

## Tips and tricks
```
