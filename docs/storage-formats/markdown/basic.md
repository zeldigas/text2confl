---
labels: supported-format,markdown
---

# Markdown - basics

[TOC]

## Text styling

All basic styling features are supported &mdash; text can be **bold**, _italic_, ~~strikethrough~~ or mixed one like
**bold with _emphasis_ part**, all  ***bold and italic***. For those who need ~subscript~ or ^superscript^ text - it is
supported as well.

You can also use quotation blocks:

> This is a quote
>
> Spanning multiple paragraphs **styled** and `monoscript`

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

Simple task lists are supported too, but due to limitations of Confluence no mixed items are supported.

This will work:

* [ ] not done
* [x] done

But this will not work:

* [ ] not done
* [x] done
* simple item

## Emojis :rocket:

You can embed emojis supported in unicode by using `:emoji-code:` notation :metal:. In some cases it is way easier
compared to putting just unicode symbol.

You can :mag_right: supported codes on [emoji cheat sheet](https://www.webfx.com/tools/emoji-cheat-sheet/) page.
