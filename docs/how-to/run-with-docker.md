# Run with Docker

This guide shows how to run text2confl using the official Docker image. This is the recommended approach for CI/CD pipelines.

## Image

The official image is `zeldigas/text2confl`. Tags:
- `zeldigas/text2confl:latest` - latest release from `master`
- `zeldigas/text2confl:0.3.0` - a specific version

## Basic usage

Mount your docs directory as a volume and pass credentials via environment variable:

```shell
docker run --rm -it \
  -v "$PWD:/wd" \
  --workdir /wd \
  -e CONFLUENCE_ACCESS_TOKEN=your_token \
  zeldigas/text2confl upload --docs .
```

## Using a credentials file

If you store credentials in `~/.config/text2confl/config.properties`, mount the config directory read-only:

```shell
docker run --rm -it \
  -v ~/.config/text2confl:/root/.config/text2confl:ro \
  -v "$PWD:/wd" \
  --workdir /wd \
  zeldigas/text2confl upload --docs .
```

## CI/CD example (GitHub Actions)

```yaml
- name: Publish docs to Confluence
  run: |
    docker run --rm \
      -v "${{ github.workspace }}/docs:/wd" \
      --workdir /wd \
      -e CONFLUENCE_ACCESS_TOKEN=${{ secrets.CONFLUENCE_TOKEN }} \
      zeldigas/text2confl upload --docs .
```

## See also

- [Authenticate with Confluence](./authenticate.md) - credential options
- [Upload docs to Confluence](./upload-docs.md) - upload command options
