---
labels: how-to
---

# Authenticate with Confluence Cloud using scoped tokens

Scoped tokens let you restrict exactly which API permissions a token grants — a safer alternative to a classic
all-access API token. Atlassian supports two ways to create them: as a personal user token (simplest) or as an OAuth 2.0
app token (for service accounts and automation). Both require a different server URL from the standard Confluence Cloud
address.

## Step 1: Find your Cloud ID

The `api.atlassian.com` endpoint (used with scoped tokens) requires a Cloud ID instead of your site hostname.

```shell
curl https://<yoursite>.atlassian.net/_edge/tenant_info
```

The response contains your Cloud ID:

```json
{
  "cloudId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "siteUrl": "https://yoursite.atlassian.net"
}
```

Copy the `cloudId` value.

## Step 2: Configure the server URL

Set the server URL to `https://api.atlassian.com/ex/confluence/<cloudId>` — note there is no `/wiki` suffix and no site
hostname.

In `.text2confl.yml`:

```yaml
server: https://api.atlassian.com/ex/confluence/<cloudId>
```

Or as a CLI flag:

```shell
text2confl upload --docs . --confluence-url https://api.atlassian.com/ex/confluence/<cloudId> ...
```

text2confl automatically detects this URL format and enables scoped token mode. When you run `doctor confluence`, hints
will name the exact missing OAuth scope if a check fails.

## Step 3: Create a scoped token

Choose the approach that fits your use case.

### Option A — User scoped token (recommended for individuals)

Go to [Atlassian account security settings](https://id.atlassian.com/manage-profile/security/api-tokens) — the same page
used for classic API tokens. Create a new token and select **Scoped** when prompted. Grant it the following scopes:

| Category | Scope                                  |
|----------|----------------------------------------|
| Read     | `read:space:confluence`                |
| Read     | `read:page:confluence`                 |
| Read     | `read:hierarchical-content:confluence` |
| Read     | `read:attachment:confluence`           |
| Read     | `read:label:confluence`                |
| Write    | `write:page:confluence`                |
| Write    | `write:label:confluence`               |
| Write    | `write:confluence-file`                |
| Delete   | `delete:page:confluence`               |
| Delete   | `delete:attachment:confluence`         |

Save the generated token.

### Option B — OAuth 2.0 app token (for service accounts and CI/CD)

Go to the [Atlassian developer console](https://developer.atlassian.com/console/myapps/) and create an OAuth 2.0 (3LO)
app or a machine-to-machine token. Grant it the same set of scopes listed in option A.

Save the generated access token.

> **Note:** `write:page:confluence` covers page property operations — no separate property scope is needed.
`write:confluence-file` is a classic scope (no granular equivalent) and covers attachment uploads.

## Step 4: Pass the access token

Use `--access-token` (no username needed):

```shell
text2confl upload --docs . \
  --confluence-url https://api.atlassian.com/ex/confluence/<cloudId> \
  --access-token YOUR_SCOPED_TOKEN \
  --space MYSPACE
```

In CI/CD, set `CONFLUENCE_ACCESS_TOKEN` and `CONFLUENCE_URL` environment variables instead:

| CLI argument       | Environment variable      |
|--------------------|---------------------------|
| `--access-token`   | `CONFLUENCE_ACCESS_TOKEN` |
| `--confluence-url` | `CONFLUENCE_URL`          |

## See also

- [Authenticate with Confluence](./authenticate.md) — classic API token and username/password
- [Diagnose Confluence access with doctor confluence](./troubleshoot-with-doctor.md) — verify your scoped token has all
  required permissions
