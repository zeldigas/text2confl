# Enable auto-fix for HTML content

This guide shows how to enable automatic fixing of invalid HTML in converted content.

For background on why this is sometimes needed, see [Content auto-fix](../explanation/auto-fix-content.md).

## Enable in configuration file

Add the following to your `.text2confl.yml`:

```yaml
auto-fix-content-tags: true
```

This enables auto-fix for all runs in this documentation root.

## Enable for a single run

Pass the flag to the upload command:

```shell
text2confl upload --docs . --auto-fix-content
```

## See also

- [Content auto-fix](../explanation/auto-fix-content.md) — when to enable it and the tradeoffs
