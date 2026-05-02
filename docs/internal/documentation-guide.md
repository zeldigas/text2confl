# Documentation Guide

Docs use the [Diataxis](https://diataxis.fr) framework — four quadrants, each with a distinct purpose:

| Quadrant | Folder | Purpose |
|----------|--------|---------|
| Tutorial | `tutorials/` | Hands-on learning for newcomers |
| How-to | `how-to/` | Goal-oriented task guides |
| Explanation | `explanation/` | Conceptual background / "why" |
| Reference | `reference/` | Accurate lookup material |

## Conventions

- **Front matter required** on every page: `labels: <quadrant>` (values: `tutorial`, `how-to`, `explanation`, `reference`, `contributing`)
- **Index files** at `docs/<quadrant>.md` list all pages in that quadrant — update when adding a page
- **Formats**: `.md` and `.adoc` coexist; format choice by content type:
  - Tutorial, how-to, explanation — prefer AsciiDoc (more feature-rich)
  - Reference for a specific format — use that format (markdown reference in `.md`, AsciiDoc reference in `.adoc`)
- **Shared AsciiDoc snippets**: prefix with `_` (e.g. `_common-setup.adoc`) — not published as standalone pages

## Adding a Page

1. Pick the correct quadrant (tutorial = learning path; how-to = specific task; explanation = background; reference = lookup)
2. Create file in `docs/<quadrant>/`
3. Add front matter with correct `labels` value
4. Add link in the quadrant's index file (`docs/<quadrant>.md`)
5. Add link in root `README.md` if user-facing

## Confluence Publishing

Config: `docs/.text2confl.yml` (targets Confluence Cloud). Publish with:

```shell
text2confl upload --docs ./docs --confluence-url <url> --user <user> --password <token> --space <SPACE> --parent "<parent>"
```
