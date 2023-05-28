---
labels: supported-format,markdown
---

# Markdown - links, images and other attachments

## Links

[**text2confl**](https://github.com/zeldigas/text2confl) supports both [external] links as well cross-linking to
another [pages](../../storage-formats.md) or [anchors inside page](../../storage-formats.md#markdown). If you need to
put a
link to [anchor on same page](#images) it is supported too.

## Images

You can embed images - both external and attached to page. Images support attributes that you can use to customize their
alignment and width/height.

External:

![Octocat](https://myoctocat.com/assets/images/base-octocat.svg)

Attached:

![markdown logo](./markdown.png){width=200}

[external]: https://example.org

You can find details about supported attributes
on [page "Controlling images representation"](../../user-guide/image-attributes.md)

## Page attachments

Besides images, you can attach to page any other file, like [simple text file](_assets/sample_file.txt).

**text2confl** collects all references pointing to local files, no matter if there is a link on page to reference or
not.

When you need to attach a file, but without link in text, use link reference that is not used on page:

```markdown
[another_file]: _assets/attached_not_referenced.txt
```

This will create attachment `another_file` without putting link in page content.

Reference definitions also let you control the name of attachment. For inline links it is calculated automatically. Here
is [a link to another attachment][custom_name] with custom name `custom_name`.

[another_file]: _assets/attached_not_referenced.txt

[custom_name]: _assets/sample_file.txt