# Manage orphaned pages

When you remove a document from your docs directory, the corresponding Confluence page is no longer part of the uploaded tree. text2confl calls these **orphaned pages** and can clean them up automatically.

## Cleanup modes

There are three cleanup modes, controlled by the `remove-orphans` configuration option or the `--remove-orphans` CLI flag:

| Mode | Config value | Description |
|------|-------------|-------------|
| Delete managed pages (default) | `managed` | Removes only pages that were uploaded by text2confl (marked as "managed") |
| Delete all orphaned pages | `all` | Removes any page under the upload root that is not part of the current document tree |
| Do nothing | `none` | Leaves orphaned pages in place — no cleanup |

## What is a managed page?

A page is **managed** if its content was last written by text2confl. text2confl stores a marker in the page metadata during upload. Pages created or last edited manually in Confluence are not considered managed.

This distinction protects manually-maintained pages from being deleted when using the default cleanup mode.

## Configure cleanup mode

In `.text2confl.yml`:

```yaml
remove-orphans: managed   # default
# remove-orphans: all
# remove-orphans: none
```

Or on the command line:

```shell
# Delete only managed orphans (default behavior — explicit)
text2confl upload --docs . --remove-orphans managed

# Delete all orphaned pages under the upload root
text2confl upload --docs . --remove-orphans all

# Disable cleanup entirely
text2confl upload --docs . --remove-orphans none
```

## Multi-tenancy and cleanup

When multi-tenancy is configured, managed-page cleanup (`managed` mode) skips pages that belong to a different tenant. The `all` mode still removes pages regardless of tenant.

See [Configure multi-tenancy](./configure-multi-tenancy.md) for details.

## See also

- [Configuration reference](../reference/configuration.md) — `remove-orphans` option
