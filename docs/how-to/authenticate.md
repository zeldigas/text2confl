# Authenticate with Confluence

This guide shows how to provide credentials to text2confl for connecting to your Confluence instance.

## Authentication methods

text2confl supports two methods:

1. **Access token** - pass `--access-token` alone (no username needed)
2. **Username + password** - pass `--user` and `--password`. If `--user` is provided but `--password` is omitted,
   text2confl prompts for it interactively.

## Getting credentials

**Confluence Cloud:** Create an API token
at [Atlassian account security settings](https://id.atlassian.com/manage-profile/security/api-tokens). Use your email as
the username and the token as the password:

```shell
text2confl upload --docs . --user your@email.com --password YOUR_API_TOKEN
```

**Confluence Server/Data Center:** Create a personal access token in your Confluence profile settings under "Personal
Access Tokens" (see
the [official guide](https://confluence.atlassian.com/enterprise/using-personal-access-tokens-1026032365.html)).
Pass it with `--access-token`:

```shell
text2confl upload --docs . --access-token YOUR_PERSONAL_ACCESS_TOKEN
```

Alternatively, use username and password:

```shell
text2confl upload --docs . --user yourname --password yourpassword
```

## Storing credentials persistently

**On a developer machine** — store credentials in a properties file so you don't have to type them on every run.
text2confl checks these locations in order:

1. `$XDG_CONFIG_HOME/text2confl/config.properties` (defaults to `~/.config/text2confl/config.properties`)
2. `text2confl.properties` in the current directory

```properties
upload.user=your@email.com
upload.password=YOUR_API_TOKEN
```

**In CI/CD** — use environment variables:

| CLI argument     | Environment variable      |
|------------------|---------------------------|
| `--access-token` | `CONFLUENCE_ACCESS_TOKEN` |
| `--user`         | `CONFLUENCE_USER`         |
| `--password`     | `CONFLUENCE_PASSWORD`     |
