## Links

[**text2confl**](https://github.com/zeldigas/text2confl) supports both [external](https://example.org) links as well cross-linking to another [pages](wiki://DOCS/[text2confl] Storage formats) or [anchors inside page](wiki://DOCS/[text2confl] Storage formats#markdown). If you need to put a link to [anchor on same page](#images) it is supported too.

Simplified page links also supported: [Sample page](wiki://null/Sample page)

## Images

You can embed images - both external and attached to page. Images support attributes that you can use to customize their alignment and width/height.

External:

![Octocat](https://myoctocat.com/assets/images/base-octocat.svg)

Attached:

![markdown logo][markdown.png]{width=200}

## Page attachments

Besides images, you can attach to page any other file, like [simple text file][_assets_sample_file.txt].

**text2confl** collects all references pointing to local files, no matter if there is a link on page to reference or not.

If for some reason you need to attach a file, but without link in text, use link reference that is not used on page:

```
[another_file]: _assets/attached_not_referenced.txt
```

This will create attachment `another_file` without putting link in page content.

Reference definitions also let you control the name of attachment. For inline links it is calculated automatically.
