---
labels: how-to
---

# Diagnose Confluence access with `doctor confluence`

`doctor confluence` runs 16 API checks against your Confluence instance — verifying connectivity, authentication, space
access, and permissions for every operation text2confl needs. Run it before your first upload or when diagnosing a
broken setup.

## Running the command

```shell
text2confl doctor confluence \
  --confluence-url https://mysite.atlassian.net/wiki \
  --access-token YOUR_TOKEN \
  --space MYSPACE \
  --parent-id 123456
```

Alias: `text2confl dr confl` is equivalent.

**Options specific to this command:**

| Option        | Description                                                                          |
|---------------|--------------------------------------------------------------------------------------|
| `--space`     | Space key to test against (required if not set in config)                            |
| `--parent-id` | ID of the parent page to create the test page under (takes priority over `--parent`) |
| `--parent`    | Title of the parent page to create the test page under                               |
| `--config`    | Directory containing `.text2confl.yml` (defaults to current directory)               |

All standard connection options also apply: `--confluence-url`, `--access-token`, `--user`, `--password`,
`--confluence-cloud`, `--skip-ssl-verification`, etc. Options not on the command line are loaded from `.text2confl.yml`
when `--config` is set.

If neither `--parent-id` nor `--parent` is provided, the test page is created under the space homepage.

## What it checks

The command runs 16 steps in sequence. If a step fails, all steps that depend on it are automatically skipped.

| Step                  | What it verifies                                     |
|-----------------------|------------------------------------------------------|
| `describe-space`      | Space exists and is accessible                       |
| `get-page`            | Parent page can be read                              |
| `create-page`         | Pages can be created in the space                    |
| `find-child-pages`    | Child pages can be listed (hierarchical read access) |
| `add-labels`          | Labels can be added to pages                         |
| `read-labels`         | Labels can be read back from pages                   |
| `create-property`     | Page properties can be created                       |
| `add-attachment`      | Files can be attached to pages                       |
| `download-attachment` | Attachments can be downloaded                        |
| `update-page`         | Pages can be updated                                 |
| `rename-page`         | Pages can be renamed                                 |
| `update-property`     | Page properties can be updated                       |
| `update-attachment`   | Attachments can be replaced                          |
| `delete-label`        | Labels can be removed from pages                     |
| `delete-attachment`   | Attachments can be deleted                           |
| `delete-page`         | Pages can be deleted                                 |

The test page and all artifacts are deleted at the end. The cleanup steps (`delete-attachment`, `delete-page`) always
run even when earlier steps fail.

## Reading the output

```
[ OK  ] describe-space
[ OK  ] get-page
[ OK  ] create-page
[FAIL ] add-labels
         Error: 403 Forbidden
         Hint:  Scoped token needs 'write:label:confluence' scope.
[SKIP ] read-labels
...

1 check(s) failed, 3 passed, 12 skipped.
```

- `[ OK  ]` — the check passed
- `[FAIL ]` — the check failed; read the **Error** and **Hint** lines
- `[SKIP ]` — skipped because a dependency failed

## Acting on hints

The hint line is tailored to your Confluence setup:

- **Cloud with scoped token** (URL starts with `https://api.atlassian.com/ex/confluence/`): names the exact OAuth 2.0
  scope that is missing. Add that scope to your OAuth app and rerun.
- **Cloud with classic API token**: describes the Confluence space permission needed (e.g. "Add page permission in space
  X").
- **Server / Data Center**: describes the permission role required (e.g. space admin for page deletion).
- **Generic errors** (network failures, page not found): identifies what to verify — space key, parent page ID, or
  server URL.

For scoped tokens,
see [Authenticate with Confluence Cloud using OAuth 2.0 scoped tokens](./authenticate-with-scoped-tokens.md) for the
complete list of required scopes.

## If the run is interrupted

If the command exits mid-run (network failure, Ctrl+C) after `create-page` has passed, a test page may be left in your
space. The hint lines for `delete-page` and `delete-attachment` include the page ID and attachment ID — use these to
locate and delete the artifacts manually in Confluence.

## See also

- [Authenticate with Confluence](./authenticate.md)
- [Authenticate with Confluence Cloud using OAuth 2.0 scoped tokens](./authenticate-with-scoped-tokens.md)
- [Troubleshoot conversion output](./troubleshoot-conversion.md)
