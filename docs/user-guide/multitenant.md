# Multi-tenant pages

Sometimes multiple teams share space in Confluence. If teams want to manage their docs in separate repositories, a
problem with pages cleanup will happen - text2confl has only subset of all pages, so in cleanup phase it may remove
other team's page that is located under same parent.

```text {title="Example of mixed content under same parent"}
parent/
├── team-a-page
└── team-b-page
```

To solve this, you have 2 options:

1. Fallback to single repository where all docs will be located
2. Configure multi-tenancy for your uploads

## Configuring multi-tenancy

In `text2confl.yml` add `tenant` parameter:

```yaml {title="text2confl.yml}
tenant: team-a
```

Alternatively you can use `--tenant` command line argument when doing upload.

## Rules for multi-tenant pages

1. If no tenant is explicitly specified, page is "vacant" and can be associated with any specific tenant
    - Exception - virtual pages, as they are not modified
2. Page from another tenant can't be updated, it will generate error
    - Again virtual pages can be from different tenants, but invalid parent for such changes will cause error 
3. _Managed_ pages cleanup skips pages from another tenant

## Recommendations for organizing multi-tenant

1. If you have some "shared" pages, they can be uploaded without tenant at all
2. Pick some unique, but not lengthy name - 
