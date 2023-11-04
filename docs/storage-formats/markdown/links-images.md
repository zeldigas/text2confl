---
labels: supported-format,markdown
---

# Markdown - links, images and other attachments

## Links

Links of different types supported:

| Link type                                    | Markdown                                                   | Confluence                                               |
|----------------------------------------------|------------------------------------------------------------|----------------------------------------------------------|
| External                                     | `[name of the link](http://example.org)`                   | [name of the link](http://example.org)                   |
| External as [link reference][link_ref_guide] | `[name of the link][external]`                             | [name of the link][external]                             |
| Link to another page                         | `[linkg to page](../../storage-formats.md)`                | [link to page](../../storage-formats.md)                 |
| Link to another page with anchor             | `[anchors inside page](../../storage-formats.md#markdown)` | [anchors inside page](../../storage-formats.md#markdown) |
| Anchor on same page                          | `[anchor inside current page](#images)`                    | [anchor inside current page](#images)                    |

[external]: https://example.org

[link_ref_guide]: https://www.markdownguide.org/basic-syntax/#reference-style-links

## Images

You can embed images - both external and attached to page. Images support attributes that you can use to customize their
alignment and width/height.

| Link type | Markdown                                                           | Confluence                                                       |
|-----------|--------------------------------------------------------------------|------------------------------------------------------------------|
| External  | `![Octocat](https://myoctocat.com/assets/images/base-octocat.svg)` | ![Octocat](https://myoctocat.com/assets/images/base-octocat.svg) |
| Attached  | `![markdown logo](./markdown.png){width=200}`                      | ![markdown logo](./markdown.png){width=200}                      |

You can find details about supported attributes
on [page "Controlling images representation"](../../user-guide/image-attributes.md)

## Page attachments

Besides images, you can attach to page any other file:

| Markdown                                      | Confluence                                  |
|-----------------------------------------------|---------------------------------------------|
| `[simple text file](_assets/sample_file.txt)` | [simple text file](_assets/sample_file.txt) |

**text2confl** collects all references pointing to local files, no matter if there is a link on page to reference or
not.

When you need to attach a file, but without a link on page itself, use link reference that is not used on page:

```markdown
[another_file]: _assets/attached_not_referenced.txt
```

This will create attachment `another_file` without putting link in page content.

Reference definitions also let you control the name of attachment. For inline links it is calculated automatically. Here
is [a link to another attachment][custom_name] with custom name `custom_name`.

[another_file]: _assets/attached_not_referenced.txt

[custom_name]: _assets/sample_file.txt