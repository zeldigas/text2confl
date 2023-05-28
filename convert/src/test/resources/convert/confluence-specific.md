[TOC]

## Admonitions

Admonitions are supported via [custom extension](https://github.com/vsch/flexmark-java/wiki/Admonition-Extension) format and rendered as information panels as they called in Confluence Cloud or *note*, *warning*, *info*, *tip* blocks in Server edition. Note that expand functionality (block that starts with `???`) is not supported because Confluence can't configure expand for these macros (and wrapping them into separate expand section seems too much).

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

Table of contents is supported via [custom extension](https://github.com/vsch/flexmark-java/wiki/Table-of-Contents-Extension) that can be put in any document location by special `[TOC]` reference put on separate line. You can control representation of table of contents with [supported options](https://confluence.atlassian.com/doc/table-of-contents-macro-182682099.html):

| parameter |                                                  description                                                   |
|-----------|----------------------------------------------------------------------------------------------------------------|
| type      | Style of TOC: `list` (default) or `flat`                                                                       |
| maxLevel  | maximum heading level to include in toc. E.g. if specified as `3` headings 4, 5 and so on will not be included |
| outline   | Adds numbering to TOC elements (1.1, 1.2, 1.2.3 and so on). By default is disabled, to enable set to `true`    |
| minLevel  | minimum heading level to include in toc. E.g. if specified as `2` headings 1 will not be included              |
| style     | style of toc items in list: `circle`, `disc`, `square` as well as other valid css style of elements            |
| separator | Separator between items for **flat** toc style: `brackets`, `braces`, `pipe`                                   |
| indent    | Indent for a **list** toc style. Valid css size, e.g. `10px`                                                   |
| include   | Filter headings to include according to specified regex                                                        |
| exclude   | Filter headings to exclude according to specified regex                                                        |
| class     | Css class to add to wrapping div where toc is put                                                              |

Example of table of contents that is put after foreword block:

```
This page will tell you about important widget `FOO` usage in our project

[TOC maxLevel=2]

## Setting up

....

## Using in project

....

## Tips and tricks
```

## Status

Status is a specific element that can serve as eye candy element for various reporting: <status color="green">on track</status>, <status color="grey">on hold</status>, <status color="red">off track</status>

For this you need to put custom tag: `<status color="$color">$text_of_status</status>`, where `$color` is valid color and `$text_of_status` is simple text that will be put in block.

Note that only limited colors are supported, and you need to properly specify one of the following allowed values: `grey`, `red`, `green`, `purple`, `blue`.

## Mentioning user (Confluence Server only)

You can mention user using `@username` format just like you can do on GitHub or in WYSIWYG Confluence editor. Unfortunately due to absence of human-readable usernames in Cloud edition this will work only on Server/Datacenter where human-readable usernames are still supported. If you still need to mention user in Cloud, consider using [raw confluence markdown](#adding-raw-confluence-formatting)

## Putting date

To put a date that is rendered in fancy way in Confluence, standard html tag is used - `<time datetime="YYYY-MM-DD" />`, e.g. <time datetime="2022-02-15"></time>. If you need this date to be rendered not just on Confluence page, consider using more standard format of this tag by putting text inside time block: <time datetime="2022-02-15">Feb 15th</time>

## Expand blocks

You can use admonition-like syntax to add Confluence expand block:

!!! expand

    I'm text that is put inside expand block

## Confluence macros with simple options

Confluence has a lot of [*macros*](https://confluence.atlassian.com/doc/macros-139387.html) - special gadgets that can add extra features to your confluence page. While some of them has comprehensive configuration or can embed text content (like expand block), a lot of macros are as simple as macro keyword and a number of options that helps you configure behavior.

**text2confl** introduce custom format that helps you to insert any macro that does not require complex parameters with `[MACRONAME key1=value1 key2=value2]` format. In markdown such format is used for link references, but just as with [table of contents](#table-of-contents) it is treated in special way if you don't define link reference somewhere down the road. Values can be unquoted if they don't contain spaces, or you can put value in quotes if you have spaces - `[MYMACRo width=100 searchQuery="project in (A,B,C)"]`.

!!! info "Parameters for macros - how to find them?"

    Parameters are ***not validated***, so make sure that you use expected params for your macro. This can be done by adding the macro you need on sample page in WYSIWYG editor and then opening page in "storage format". Macro name will be in `<ac-structured-macro ac:name="MACRONAME">` block and all `<ac-parameter ac:name="columns">` elements are macro parameters.

    This is especially helpful for special hidden parameters like `serverId` in jira chart macro, that is GUID string and unique per jira server integration.

By default, any `MACRONAME` is supported, but if you want to limit usage, you can explicitly set what macros are enabled with this notation. More details on this in [configuration reference](wiki://DOCS/[text2confl] Configuration reference)

Some examples:

|                                                                        Type of macros                                                                         |                                         Markdown text                                          |                                           Result                                            |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------|
| [Reference to single jira ticket](https://confluence.atlassian.com/doc/jira-issues-macro-139380.html#JiraIssuesMacro-Displayingasingleissue,orselectedissues) | `[JIRA key=SI-1]`                                                                              | [JIRA key=SI-1]                                                                             |
| [Jira report table](https://confluence.atlassian.com/doc/jira-issues-macro-139380.html#JiraIssuesMacro-DisplayingissuesviaaJiraQueryLanguage(JQL)search)      | `[JIRA jqlQuery="project = SI" columns=key,summary,assignee,reporter,status maximumIssues=20]` | [JIRA columns=key,summary,assignee,reporter,status maximumIssues=5 jqlQuery="project = SI"] |
| [Jira charts](https://confluence.atlassian.com/doc/jira-chart-macro-427623467.html)                                                                           | `[JIRACHART jql="project = SI" chartType=pie statType=components serverId=<JIRA_SERVER_ID>]`   | [JIRACHART jql="project = SI" statType=components chartType=pie serverId=<JIRA_SERVER_ID>]  |

## Adding raw confluence formatting

Flexmark library that is used to parse markdown follows common mark spec that prohibits html tags with colons, but this is the heart of custom Confluence markup because they use `ac:` and `ri:` as their namespace prefixes for all macro tags. To overcome this limitation, **text2confl** supports alternative format confluence tags with dashes.

So this tags

```xml
<ac-structured-macro ac:name="jira">
    <ac-parameter ac:name="columns">key,summary,assignee,reporter,status</ac-parameter>
    <ac-parameter ac:name="maximumIssues">20</ac-parameter>
    <ac-parameter ac:name="jqlQuery">project = SI</ac-parameter>
</ac-structured-macro>
```

