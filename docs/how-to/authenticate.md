# Authenticate with Confluence

This guide shows how to provide credentials to text2confl for connecting to your Confluence instance.

## Authentication methods

text2confl supports two methods:

1. **Access token** — pass `--access-token` alone (no username needed)
2. **Username + password** — pass `--user` and `--password`. If `--user` is provided but `--password` is omitted, text2confl prompts for it interactively.

## Getting credentials

**Confluence Cloud:** Create an API token at [Atlassian account security settings](https://id.atlassian.com/manage-profile/security/api-tokens). Use your email as the username and the token as the password:

```shell
text2confl upload --docs . --user your@email.com --password YOUR_API_TOKEN
```

**Confluence Server/Data Center:** Create a personal access token in your Confluence profile settings under "Personal Access Tokens". Pass it with `--access-token`:

```shell
text2confl upload --docs . --access-token YOUR_PERSONAL_ACCESS_TOKEN
```

Alternatively, use username and password:

```shell
text2confl upload --docs . --user yourname --password yourpassword
```

## Storing credentials persistently

To avoid passing credentials on every command, store them in one of these locations (checked in order):

1. `$XDG_CONFIG_HOME/text2confl/config.properties` (defaults to `~/.config/text2confl/config.properties`)
2. `text2confl.properties` in the current directory

| CLI argument     | Environment variable      | Properties file key         |
|------------------|---------------------------|-----------------------------|
| `--access-token` | `CONFLUENCE_ACCESS_TOKEN` | `upload.access-token`       |
| `--user`         | `CONFLUENCE_USER`         | `upload.user`               |
| `--password`     | `CONFLUENCE_PASSWORD`     | `upload.password`           |

Example `~/.config/text2confl/config.properties`:

```properties
upload.user=your@email.com
upload.password=YOUR_API_TOKEN
```

Environment variables are the preferred approach for CI/CD pipelines.
