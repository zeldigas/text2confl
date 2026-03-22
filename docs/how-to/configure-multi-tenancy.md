# Configure multi-tenancy

This guide shows how to configure multi-tenancy when multiple teams share the same Confluence space and manage pages under the same parent.

For background on why this is needed, see [Multi-tenancy model](../explanation/multi-tenancy-model.md).

## Configure a tenant ID

Add the `tenant` parameter to `.text2confl.yml`:

```yaml
tenant: team-a
```

Alternatively, pass it on the command line:

```shell
text2confl upload --docs . --tenant team-a
```

## Rules for multi-tenant pages

1. Pages without a tenant are "vacant" and can be claimed by any tenant — except virtual pages, which are never modified.
2. A page belonging to another tenant cannot be updated; text2confl will report an error.
3. Managed-page cleanup skips pages that belong to a different tenant.

## Recommendations

1. **Shared pages** (e.g. a space home page) can be uploaded without a tenant so all teams can read them without conflict.
2. **Pick a short, unique tenant ID** — it is stored in Confluence page metadata, so keep it stable across runs.

## See also

- [Multi-tenancy model](../explanation/multi-tenancy-model.md) — conceptual explanation of the problem and solution
- [Manage orphaned pages](./manage-orphaned-pages.md) — how cleanup interacts with tenants
