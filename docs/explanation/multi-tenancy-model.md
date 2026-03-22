---
labels: explanation
---

# Multi-tenancy model

## The problem

When multiple teams share a Confluence space and manage their docs independently, they typically publish pages under the
same parent. This creates a conflict during the orphan cleanup phase: text2confl only knows about the pages it uploaded
in the current run, so it may incorrectly identify another team's pages as orphans and delete them.

```text
parent/
├── team-a-page    ← uploaded by Team A's pipeline
└── team-b-page    ← uploaded by Team B's pipeline
```

If Team A runs an upload without Team B's page in its document tree, the default cleanup would remove `team-b-page`.

## The solution: tenant IDs

text2confl solves this with a tenant identifier. When a page is uploaded with a tenant ID, that ID is stored in the
page's Confluence metadata. During cleanup, text2confl only removes orphaned pages that belong to the same tenant -
pages from other tenants are left untouched.

## How tenant isolation works

- A page uploaded without a tenant is "vacant" - it has no tenant association. Vacant pages can be claimed by any tenant
  on a subsequent write.
- A page uploaded with tenant `team-a` can only be updated by runs using `tenant: team-a`. Attempts from other tenants
  produce an error.
- Orphan cleanup with the default `managed` mode skips pages belonging to a different tenant.
- Virtual pages are always treated as tenant-neutral: they are never modified by content uploads and are excluded from
  cleanup regardless of tenant.

## Design trade-offs

Tenant IDs are stored in Confluence page metadata, not in the document source. This means:

- The same source files can be published under different tenants by changing only the configuration, without modifying
  the documents themselves.
- Tenant identity is a property of the upload pipeline, not the content.

## See also

- [Configure multi-tenancy](../how-to/configure-multi-tenancy.md) - steps to set up tenant IDs
- [Manage orphaned pages](../how-to/manage-orphaned-pages.md) - how cleanup modes interact with tenants
