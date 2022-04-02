---
labels: supported-format,markdown
---

# Markdown

[TOC exclude="(.*\.md|.*\.kt)"]

# Document structure

Every Markdown document corresponds to separate confluence page.

Document can contain *yaml frontmatter* block at the beginning - section in YAML format where various metadata can be
defined like custom title for page or confluence labels.

Example document with *yaml* block that has 2 *attributes*: `title` and `labels`

```markdown
---
title: hello 
labels: docs,intro
---

Document content 
```

## Document title

Document title (name) can be defined in the following ways (from top priority to bottom):

1. `title` attribute in *yaml frontmatter*
2. first level heading at the top of document. If it is used, heading will be removed from resulting document to avoid
   duplication
3. name of Markdown file

Recommended approach is to use first level heading as it provides also good-looking markdown content outside of
confluence. Usage of `title` attribute also fine, especially if you prefer to hide as much confluence-specific things
from content as possible.

### Example 1 - title will be taken from *yaml frontmatter*

```markdown {title=my-page.md}
---
title: My page
---
Document content
```

Result title - `My page`

### Example 2 - title will be taken from *yaml frontmatter* regardless presence of top level header

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

## Page labels

Confluence page labels can be set by `labels` attribute in *yaml frontmatter* block

Example - document that will have 3 labels (`one`, `two`, `three`):

```markdown
---
labels: one, two, three
---
Document content
```

## Custom parent

It is possible to specify custom parent for every page using `parent` or `parentId` attributes in *yaml frontmatter*
block. While it's not restricted on every level, it makes sense to use this only for top level pages for configuring
location of subtree (root page and all its children).

Attribute `parentId` should contain id of parent page and attribute `parent` should contain parent page title. When
specified both, `parentId` is used and `parent` will be ignored.

# Supported features

## Text styling

All basic styling features are supported &mdash; text can be **bold**, _italic_, ~~strikethrough~~ or mixed one like
**bold with _emphasis_ part**, all  ***bold and italic***. For those who need ~subscript~ or ^superscript^ text - it is
supported as well.

Quotation blocks:

> This is a quote
>
> Spanning multiple paragraphs **style** and `monoscript`

## Code blocks and inlines

Inline code: `printf("Hello world!")`

Code blocks are also supported including language tags:

```java
class Test {
    public static void main(String[] args) {
        System.out.println("Hello world!");
    }
}
```

**Text2confl** is familiar with different list of supported language highlights in Confluence Server/DataCenter and Cloud,
so it will keep language from language tag if Confluence will support it and remove it when such languages are not
supported.

Some confluence specific features for code blocks, like line numbers or collapsible block are supported
with [markdown attributes](https://github.com/vsch/flexmark-java/wiki/Attributes-Extension) that you can put after
language tag

```kotlin {title="hello.kt"}
println("Hello world!")
```

or after code block (on separate line)

```kotlin
println("Same here!")
``` 

{title="hello.kt"}

List of supported attributes:

| Attribute     | Description                                                                                         | Confluence Server | Confluence Cloud                        |
|---------------|-----------------------------------------------------------------------------------------------------|-------------------|-----------------------------------------|
| `title`       | Title for codeblock                                                                                 | ✅️                | ✅️                                      |
| `collapse`    | Flag that control if code block is collapsed on page by default. Allowed values - `true` or `false` | ✅️                | ⚠️ Only if editor v1 is forcibly set ⚠️ |
| `linenumbers` | Flag that controls if line numbers are displayed. Allowed values - `true` or `false`                | ✅️                | ⚠️ Only if editor v1 is forcibly set ⚠️ |
| `firstline`   | Sets starting line number for codeblocks                                                            | ✅️                | ⚠️ Only if editor v1 is forcibly set ⚠️ |

## Links

[**Text2confl**](https://github.com/zeldigas/text2confl) supports both [external] links as well cross-linking to
another [pages](../storage-formats.md) or [anchors inside page](../storage-formats.md#markdown). If you need to put a
link to [anchor on same page](#admonitions) it is supported too.

## Images

You can embed images - both external and attached to page. Images support attributes that you can use to customize their
alignment and width/height.

External:

![Octocat](https://myoctocat.com/assets/images/base-octocat.svg)

Attached:

![markdown logo](./markdown.png){width=200}

## Lists

Both numbered and bullet lists are supported:

1. one
2. two
3. three
    * unordered
    * list
    * as nested
        1. And ordered
        2. again

Simple task lists are supported too, but due to limitations of confluence no mixed items are supported.

This will work:

* [ ] not done
* [x] done

But this will not work:

* [ ] not done
* [x] done
* simple item

## Tables

You can use tables based on [GitHub Flavored Format](https://github.github.com/gfm/#tables-extension-):

| foo | bar |
|-----|-----|
| baz | bim |

For complex cases (e.g. when you need multiline cells or complex content) you can just regular
tables (`<table>...</table>`):

<table>
<thead>
<tr>
<th>First column</th>
<th>Second column</th>
</tr>
</thead>
<tbody>
<tr>
<td>

```
code block
that you might need to put in table
```

</td>
<td>Simple text cell</td>
</tr>
</tbody>
</table>

## Admonitions

Admonitions are supported via [custom extension](https://github.com/vsch/flexmark-java/wiki/Admonition-Extension) format
and rendered as information panels as they called in Confluence Cloud or _note_, _warning_, _info_, _tip_ blocks in
Server edition. Note that expand functionality (block that starts with `???`) is not supported because Confluence can't
configure expand for these macros (and wrapping them into separate expand section seems too much).

!!! note

    A note to yourself

!!! tip

    Some tips and tricks with `rich` ~formatting~

!!! warning

    Description of pitfalls that your user
    
    Should know about

!!! info

    Information message

## Table of contents

Table of contents is supported
via [custom extension](https://github.com/vsch/flexmark-java/wiki/Table-of-Contents-Extension) that can be put in any
document location by special `[TOC]` reference put on separate line. You can control representation of table of contents
with [supported options](https://confluence.atlassian.com/doc/table-of-contents-macro-182682099.html):

| parameter | description                                                                                                    |
|-----------|----------------------------------------------------------------------------------------------------------------|
| type      | Style of TOC: `list` (default) or `flat`                                                                       |
| maxLevel  | maximum heading level to include in toc. E.g. if specified as `3` headings 4, 5 and so on will not be included |
| outline   | Adds numbering to TOC elements (1.1, 1.2, 1.2.3 and so on). By default is disabled, to enable set to `true`    |
| minLevel  | minimum heading level to include in toc. E.g. if specified as `2` headings 1 will not be included              |
| style     | style of toc items in list: `circle`, `disc`, `square` as well as other valid css style of elements            |
| separator | Separator between items for **flat*** toc style: `brackets`, `braces`, `pipe`                                  |
| indent    | Indent for a ***list** toc style. Valid css size, e.g. `10px`                                                  |
| include   | Filter headings to include according to specified regex                                                        |
| exclude   | Filter headings to exclude according to specified regex                                                        |
| class     | Css class to add to wrapping div where toc is put                                                              |


Example of table of contents that is put after foreword block:
```markdown
This page will tell you about important widget `FOO` usage in our project

[TOC maxLevel=2]

## Setting up

....

## Using in project

....

## Tips and tricks
```

## Confluence specific goodies

### Status

Status is a specific element that can serve as eye candy element for various reporting: 
<status color="green">on track</status>, <status color="grey">on hold</status>, <status color="red">off track</status>

For this you need to put custom tag: `<status color="$color">$text_of_status</status>`, where `$color` is valid color
and `$text_of_status` is simple text that will be put in block.

Note that only limited colors are supported, and you need to properly specify one of the following allowed values: `grey`
, `red`, `green`, `purple`, `blue`.

### Mentioning user (Confluence Server only)

You can mention user using `@username` format just like you can do on GitHub or in WYSIWYG Confluence editor.
Unfortunately due to absence of human-readable usernames in Cloud edition this will work only on Server/Datacenter where
human-readable usernames are still supported. If you still need to mention user in Cloud, consider
using [raw confluence markdown](#adding-raw-confluence-formatting)

### Putting date

To put a date that is rendered in fancy way in Confluence, standard html tag is used - `<time datetime="YYYY-MM-DD" />`,
e.g. <time datetime="2022-02-15" />. If you need this date to be rendered not just on Confluence page, consider using
more standard format of this tag by putting text inside time block: <time datetime="2022-02-15">Feb 15th</time>

### Expand blocks

You can use admonition-like syntax to add Confluence expand block:

!!! expand

    I'm text that is put inside expand block

### Confluence macros with simple options

Confluence has a lot of [*macros*](https://confluence.atlassian.com/doc/macros-139387.html) - special gadgets that can
add extra features to your confluence page. While some of them has comprehensive configuration or can embed text
content (like expand block), a lot of macros are as simple as macro keyword and a number of options that helps you
configure behavior.

**Text2confl** introduce custom format that helps you to insert any macro that does not require complex parameters with
`[MACRONAME key1=value1 key2=value2]` format. In markdown such format is used for link references, but just as
with [table of contents](#table-of-contents) it is treated in special way if you don't define link reference somewhere
down the road. Values can be unquoted if they don't contain spaces, or you can put value in quotes if you have spaces - 
`[MYMACRo width=100 searchQuery="project in (A,B,C)"]`. 

!!! info "Parameters for macros - how to find them?"

    Parameters are ***not validated***, so make sure that you use expected params for your macro. This can be done by 
    adding the macro you need on sample page in WYSIWYG editor and then opening page in "storage format".
    Macro name will be in `<ac-structured-macro ac:name="MACRONAME">` block and all `<ac-parameter ac:name="columns">`
    elements are macro parameters.
    
    This is especially helpful for special hidden parameters like `serverId` in jira chart macro, that is GUID string
    and unique per jira server integration.

By default, any `MACRONAME` is supported, but if you want to limit usage, you can explicitly set what macros are enabled
with this notation. More details on this in [configuration reference](../configuration-reference.md)

Some examples:

| Type of macros                                | Markdown text                                                                                  | Result                                                                                      |
|-----------------------------------------------|------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------|
| [Reference to single jira ticket][JIRA_MACRO] | `[JIRA key=SI-1]`                                                                              | [JIRA key=SI-1]                                                                             |
| [Jira report table][JIRA_MACRO_TABLE]         | `[JIRA jqlQuery="project = SI" columns=key,summary,assignee,reporter,status maximumIssues=20]` | [JIRA jqlQuery="project = SI" columns=key,summary,assignee,reporter,status maximumIssues=5] |
| [Jira charts][JIRA_CHART]                     | `[JIRACHART jql="project = SI" chartType=pie statType=components serverId=<JIRA_SERVER_ID>]`   | [JIRACHART jql="project = SI" chartType=pie statType=components serverId=<JIRA_SERVER_ID>]  |


### Adding raw confluence formatting

Flexmark library that is used to parse markdown follows common mark spec that prohibits html tags with colons, but this
is the heart of custom Confluence markup because they use `ac:` and `ri:` as their namespace prefixes for all macro tags.
To overcome this limitation, **text2confl** supports alternative format confluence tags with dashes.

So this tags

```xml
<ac-structured-macro ac:name="jira">
   <ac-parameter ac:name="columns">key,summary,assignee,reporter,status</ac-parameter>
   <ac-parameter ac:name="maximumIssues">20</ac-parameter>
   <ac-parameter ac:name="jqlQuery">project = SI</ac-parameter>
</ac-structured-macro>
```

Will generate

<ac-structured-macro ac:name="jira">
<ac-parameter ac:name="columns">key,summary,assignee,reporter,status</ac-parameter>
<ac-parameter ac:name="maximumIssues">20</ac-parameter>
<ac-parameter ac:name="jqlQuery">project = SI</ac-parameter>
</ac-structured-macro>

Right now this can't be used for block macros e.g. setting up page layouts.

[external]: https://example.org
[JIRA_MACRO]: https://confluence.atlassian.com/doc/jira-issues-macro-139380.html#JiraIssuesMacro-Displayingasingleissue,orselectedissues
[JIRA_MACRO_TABLE]: https://confluence.atlassian.com/doc/jira-issues-macro-139380.html#JiraIssuesMacro-DisplayingissuesviaaJiraQueryLanguage(JQL)search
[JIRA_CHART]: https://confluence.atlassian.com/doc/jira-chart-macro-427623467.html