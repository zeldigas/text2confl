---
labels: supported-format,markdown
---

# Markdown - basics

[TOC]

## Text styling

All basic styling features are supported &mdash; text can be 

| Markdown                                                                   | Confluence                                                                |
|----------------------------------------------------------------------------|---------------------------------------------------------------------------|
| `**bold**`                                                                 | **bold**                                                                  |
| `_italic_`                                                                 | _italic_                                                                  |
| `~~strikethrough~~`                                                        | ~~strikethrough~~                                                         |
| `With ~subscript~ and ^superscript^`                                       | With ~subscript~ and ^superscript^                                        |
| `Mixed one like **bold with _emphasis_ part**, all  ***bold and italic***` | Mixed one like  **bold with _emphasis_ part**, all  ***bold and italic*** |


You can also use quotation blocks:

<table>
<thead>
<tr><th>Markdown</th><th>Confluence</th></tr>
</thead>
<tbody><tr>
<td>

```markdown
> This is a quote
>
> Spanning multiple paragraphs **styled** and `monoscript`
```

</td>
<td>

> This is a quote
>
> Spanning multiple paragraphs **styled** and `monoscript`

</td>
</tr></tbody>
</table>


## Lists

Both numbered and bullet lists are supported:

<table>
<thead>
<tr><th>Markdown</th><th>Confluence</th></tr>
</thead>
<tbody><tr>
<td>

```markdown
1. one
2. two
3. three
    * unordered
    * list
    * as nested
        1. And ordered
        2. again
```

</td>
<td>

1. one
2. two
3. three
   * unordered
   * list
   * as nested
      1. And ordered
      2. again

</td>
</tr></tbody>
</table>

Simple task lists are supported too, but due to limitations of Confluence no mixed items are supported.

This will work:

<table>
<thead>
<tr><th>Markdown</th><th>Confluence</th></tr>
</thead>
<tbody><tr>
<td>

```markdown
* [ ] not done
* [x] done
```
</td><td>

* [ ] not done
* [x] done

</td></tr></tbody></table>

But this will not work:

<table>
<thead>
<tr><th>Markdown</th><th>Confluence</th></tr>
</thead>
<tbody><tr>
<td>

```markdown
* [ ] not done
* [x] done
* simple item
```
</td><td>

* [ ] not done
* [x] done
* simple item

</td></tr></tbody></table>

## Emojis :rocket:

You can embed emojis supported in unicode by using `:emoji-code:`. In some cases it is way easier
compared to putting just unicode symbol.

| Markdown  | Confluence |
|-----------|------------|
| `:metal:` | :metal:    |


You can find supported codes on [emoji cheat sheet](https://www.webfx.com/tools/emoji-cheat-sheet/) page.
