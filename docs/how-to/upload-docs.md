# Upload docs to Confluence

This guide shows how to upload a documentation directory to Confluence.

## Upload all docs

From your documentation root directory:

```shell
text2confl upload --docs .
```

## Dry run

Use `--dry` to preview changes without modifying Confluence. All changes are logged with a `(dryrun)` marker:

```shell
text2confl upload --docs . --dry
```

## Convert without uploading

To convert documents to Confluence Storage Format and write them as local files (without uploading):

```shell
text2confl convert --docs .
```

This is useful for inspecting the output or for integrations where you handle the upload step yourself.

## See also

- [Run with Docker](./run-with-docker.md) — run text2confl in containers and CI/CD pipelines
- [Upload a single page ad-hoc](./upload-adhoc.md) — upload without a config file
- [Configuration reference](../reference/configuration.md) — all upload command options
