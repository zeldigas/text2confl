# Auto-fix converted HTML content

Confluence expects page content to be valid, well-formed XML (in Confluence Storage Format). However, Markdown and
AsciiDoc converters may sometimes produce HTML output that browsers tolerate but is not strictly valid XML — for
example, misnested or unclosed tags. When text2confl encounters such malformed content during validation, it will reject
the page and fail the upload.

**text2confl** provides an opt-in feature to automatically fix and normalize converted HTML content before validation,
allowing you to work around converter quirks and upload pages successfully even when the intermediate HTML isn't
perfectly formed. [Issue #182](https://github.com/zeldigas/text2confl/issues/182) provides example of such problem with
Asciidoctor.

## How it works

When enabled, text2confl will autofix content tag structure using Jsoup when invalid tags are detected in converted
document.

Jsoup's parser is tolerant and will normalize misnested or unclosed tags, producing well-formed HTML that can be
validated and further processed by the conversion pipeline.

## When to enable

This option is particularly useful if you have a **large team** working on multiple pages where contributors may not be
familiar with proper XML/HTML formatting rules — especially when they like to insert raw HTML snippets into Markdown or
AsciiDoc documents. Enabling auto-fix can prevent upload failures caused by minor formatting mistakes and keep your
documentation pipeline running smoothly.

Other scenarios where enabling auto-fix makes sense:

- You see conversion failures caused by invalid/misnested HTML produced by your source renderer (Asciidoctor, Markdown),
  and enabling auto-fix is a pragmatic workaround.
- You're migrating legacy content with inconsistent HTML quality.

**When to keep it disabled:**

- If you require strict correctness and want to detect problems upstream, keep this disabled so invalid HTML is surfaced
  and can be fixed at the source.
- If you have full control over content quality and prefer strict validation.

## Enabling auto-fix

### Configuration file

Add the following property to your `.text2confl.yml` (directory config) to enable the automatic fixer across runs:

```yaml
# Enable automatic content fixing (off by default)
auto-fix-content-tags: true
```

The schema for this option is `auto-fix-content-tags` (boolean) and is defined in `docs/config.schema.json`.

### CLI flag

You can enable the fixer for a single run with the upload command:

```bash
text2confl upload --confluence-url https://your-site.atlassian.net/wiki --space MYSPACE --docs ./docs --auto-fix-content
```