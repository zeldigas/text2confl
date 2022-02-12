# Markdown

[TOC]

# Document structure

# Supported features

## Text styling

All basic styling features are supported &mdash; text can be **bold**, _italic_, ~~strikethrough~~ or mixed one like **
bold with _emphasis_ part**, all  ***bold and italic***. For those who need ~subscript~ or ^superscript^ text - it is
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

Text2confl is familiar with different list of supported language highlights in Confluence Server/DataCenter and Cloud,
so it will keep language from language tag if Confluence will support it and remove it when such languages is not
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
| `collapse`    | Flag that control if code block is collapsed on page by default. Allowed values - `true` or `false` | ✅️                | ⚠️ Only if editor v1 is forcebly set ⚠️ |
| `linenumbers` | Flag that controls if line numbers are displayed. Allowed values - `true` or `false`                | ✅️                | ⚠️ Only if editor v1 is forcebly set ⚠️ |
| `firstline`   | Sets starting line number for codeblocks                                                            | ✅️                | ⚠️ Only if editor v1 is forcebly set ⚠️ |

## Links

[Text2confl](https://github.com/zeldigas/text2confl) supports both [external] links as well cross linking to
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

You can use tables based on [Github Flavored Format](https://github.github.com/gfm/#tables-extension-):

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

## Confluence specific goodies

### Status

Status is a specific element that can serve as eye candy element for varios reporting: <status color="green">on
track</status>, <status color="grey">on hold</status>, <status color="red">off track</status>

For this you need to put custom tag: `<status color="$color">$text_of_status</status>`, where `$color` is valid color
and `$text_of_status` is simple text that will be put in block.

Note that only limited colors are supported and you need to properly specify one of the following allowed values: `grey`
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

You can use admonition-like syntax to add Confluence expand block

### Adding raw confluence formatting

Flexmark library that is used to parse markdown follows common mark spec that prohibits html tags with colons, but this
is the heart of custom Confluence markup becase they use `ac:` and `ri:` as their namespace prefices for all macro tags.
To overcome this limitation, text2confl supports alternative format confluence tags with dashes.

So to put a link to jira issue (e.g. <ac-structured-macro ac:name="jira" ac:schema-version="1">
<ac-parameter ac:name="key">SI-1</ac-parameter></ac-structured-macro>), you can use the following notation:

```xml
<ac-structured-macro ac:name="jira" ac:schema-version="1"><ac-parameter ac:name="key">SI-1</ac-parameter></ac-structured-macro>
```

Right now this can't be used for block macros e.g. setting up page layouts.

[external]: https://example.org