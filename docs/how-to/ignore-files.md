---
labels: how-to
---

# Ignore specific files from being published

This guide shows how to exclude specific files in your docs directory from being converted and
published as pages.

## Ignore files by prefix

Any file whose name starts with `_` is always ignored - no configuration needed. This is useful
for partials/includes that are meant to be reused by other pages rather than published on their
own.

## Ignore specific filenames

Use `additional-files-to-ignore` in `.text2confl.yml` to ignore files by exact name (matched
case-insensitively), regardless of prefix. This is useful for excluding files that live in your
docs directory for other tooling but shouldn't be published, such as agent instruction files:

```yaml
additional-files-to-ignore:
  - CLAUDE.md
  - AGENTS.md
```

## See also

- [Configuration reference](../reference/configuration.adoc) - all configuration options
