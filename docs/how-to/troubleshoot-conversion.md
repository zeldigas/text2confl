---
labels: how-to
---

# Troubleshoot conversion output

If a published page looks wrong in Confluence, use the `convert` command to inspect the generated
Confluence Storage Format locally before uploading.

## Inspect converted output

Run `convert` from your documentation root:

```shell
text2confl convert --docs .
```

text2confl writes the converted files to a local directory (default: `out/`) without touching Confluence.
Open the output files to see the raw XML that would be uploaded, and compare it against what you expect.

## Matching the editor version

When converting a directory, text2confl reads the editor version from `.text2confl.yml` - the same as `upload`.

If you are converting a single file directly (without a config file), the editor version defaults to `v2`.
Pass `--editor-version v1` explicitly if your Confluence instance uses the legacy editor:

```shell
text2confl convert --file my-page.md --editor-version v1
```

The editor version affects table rendering, list item markup, and image attributes - so a mismatch can produce
output that looks correct locally but renders differently in Confluence.

## See also

- [Configuration reference](../reference/configuration.adoc) - `convert` command options and output directory setting
