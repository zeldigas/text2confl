# Content auto-fix

Confluence expects page content to be valid, well-formed XML in Confluence Storage Format. However, Markdown and AsciiDoc converters may produce HTML that browsers accept but is not valid XML — for example, misnested tags or unclosed elements. When text2confl encounters such content during validation, it rejects the page and fails the upload.

## Why this happens

The root cause is the gap between HTML parsing (lenient) and XML parsing (strict). Browser HTML parsers are deliberately tolerant — they recover from malformed markup. XML parsers are not. Confluence Storage Format is XML, so the same content that renders correctly in a browser can fail strict XML validation.

Markdown and AsciiDoc rendering libraries are typically tested against browser rendering, not XML validity. Some edge cases — especially raw HTML snippets embedded in Markdown, or certain Asciidoctor output — can produce structurally invalid markup that passes rendering tests but breaks XML validation. [Issue #182](https://github.com/zeldigas/text2confl/issues/182) is an example of this with Asciidoctor.

## How the auto-fix feature works

When enabled, text2confl runs the converted HTML through [Jsoup](https://jsoup.org/) before XML validation. Jsoup's parser is tolerant and normalizes misnested or unclosed tags, producing well-formed HTML. The normalized output is then passed to the rest of the conversion pipeline.

## When to enable it

Enable auto-fix when:

- You see upload failures due to invalid or misnested HTML produced by your source renderer
- You have a large team and contributors occasionally embed raw HTML snippets in documents
- You are migrating legacy content with inconsistent HTML quality

Keep it disabled when:

- You prefer to detect and fix HTML problems at the source rather than masking them
- You have full control over content quality and want strict validation to catch issues early

## See also

- [Enable auto-fix for HTML content](../how-to/enable-auto-fix.md) — steps to turn it on
