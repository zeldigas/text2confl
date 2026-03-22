# Export a Confluence page to Markdown

This guide shows how to use the `export-to-md` command to download an existing Confluence page as a Markdown file. This is useful for bootstrapping a docs-as-code migration from content already in Confluence.

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

Use `--assets-dir` to specify a subdirectory (relative to `--dest`) where page attachments will be saved:

```shell
text2confl export-to-md \
  --confluence-url https://yoursite.atlassian.net/wiki \
  --access-token YOUR_TOKEN \
  --page-id 12345678 \
  --dest ./docs \
  --assets-dir _assets
```

## Also save the raw Confluence Storage Format

Pass `--dump-also-storage-format` to save the original Confluence XML alongside the Markdown output. Useful for debugging conversion issues:

```shell
text2confl export-to-md \
  --confluence-url https://yoursite.atlassian.net/wiki \
  --access-token YOUR_TOKEN \
  --page-id 12345678 \
  --dest ./docs \
  --dump-also-storage-format
```

## Authentication

`export-to-md` accepts the same authentication flags as the `upload` command. See [Authenticate with Confluence](./authenticate.md) for all options, including environment variables.

## See also

- [Authenticate with Confluence](./authenticate.md)
- [Getting started](../tutorials/getting-started.md) — set up a full docs-as-code project after exporting your pages
