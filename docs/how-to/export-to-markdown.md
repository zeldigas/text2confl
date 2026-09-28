---
labels: how-to
---

# Export a Confluence page to Markdown

This guide shows how to use the `export-to-md` command to download an existing Confluence page as a Markdown file. This
is useful for bootstrapping a docs-as-code migration from content already in Confluence.

## Export by page ID

```shell
text2confl export-to-md \
  --confluence-url https://yoursite.atlassian.net/wiki \
  --access-token YOUR_TOKEN \
  --page-id 12345678 \
  --dest ./docs
```

The exported file is written to the `--dest` directory.

## Export by page title

If you don't know the page ID, use `--page-title` together with `--space`:

```shell
text2confl export-to-md \
  --confluence-url https://yoursite.atlassian.net/wiki \
  --access-token YOUR_TOKEN \
  --space MYSPACE \
  --page-title "My existing page" \
  --dest ./docs
```

`--page-id` takes priority over `--page-title` if both are provided.

## Save attachments

By default, page attachments are saved to `--dest`. Use `--assets-dir` to save them to a subdirectory instead (relative to `--dest`):

```shell
text2confl export-to-md \
  --confluence-url https://yoursite.atlassian.net/wiki \
  --access-token YOUR_TOKEN \
  --page-id 12345678 \
  --dest ./docs \
  --assets-dir _assets
```

## Also save the raw Confluence Storage Format

Pass `--dump-also-storage-format` to save the original Confluence XML alongside the Markdown output. Useful for
debugging conversion issues:

```shell
text2confl export-to-md \
  --confluence-url https://yoursite.atlassian.net/wiki \
  --access-token YOUR_TOKEN \
  --page-id 12345678 \
  --dest ./docs \
  --dump-also-storage-format
```

## What gets converted

Most of the regular formatting is converted to standard Markdown. Some Confluence-specific elements are converted to
text2confl [Markdown extensions](../reference/markdown/confluence-specific.md):

- **User mentions** are exported as `@username` for Confluence Server/Data Center and as `@"email"` for Confluence
  Cloud. In Cloud the email is available only if user's profile visibility settings allow it for the account used for
  export; otherwise the mention is skipped. Scoped tokens need `read:content-details:confluence` scope to look up
  users; without it a warning is logged and mentions are skipped.
- **Info, note, tip, warning panels** are exported as admonitions (`!!! note`). This also works for panels created in
  Confluence Cloud editor, but panel customizations (custom colors, icons) are not preserved.

## Authentication

`export-to-md` accepts the same authentication flags as the `upload` command.
See [Authenticate with Confluence](./authenticate.md) for all options, including environment variables.

## See also

- [Authenticate with Confluence](./authenticate.md)
- [Getting started](../tutorials/getting-started.md) - set up a full docs-as-code project after exporting your pages
