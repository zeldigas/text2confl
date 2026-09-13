# AGENTS.md



Guidance for working on files under `docs/`.

## Structure

Docs follow the [Diataxis](https://diataxis.fr) model — four quadrants, each its own folder:

| Quadrant | Folder | Purpose |
|----------|--------|---------|
| Tutorial | `tutorials/` | Hands-on learning for newcomers |
| How-to | `how-to/` | Goal-oriented task guides |
| Explanation | `explanation/` | Conceptual background / "why" |
| Reference | `reference/` | Accurate lookup material |

Format-specific syntax references live one level deeper: `reference/markdown/` and `reference/asciidoc/`, each with
its own `AGENTS.md` covering the authoring pattern used there (source/rendered-output comparisons).

## Conventions

- Every page needs YAML/AsciiDoc front matter with `labels: <quadrant>` (values: `tutorial`, `how-to`, `explanation`,
  `reference`, `contributing`).
- `docs/<quadrant>.md` is an index page listing every page in that quadrant — update it when adding a page.
- Format choice by content type: tutorials/how-to/explanation prefer AsciiDoc (more feature-rich); a
  format-specific reference page uses that format (Markdown reference in `.md`, AsciiDoc reference in `.adoc`).
- Files/directories prefixed with `_` (e.g. `_assets/`, `_common-setup.adoc`) are excluded from publishing —
  Confluence never gets a standalone page for them. Use this for shared snippets and asset folders.

## Adding a page

1. Pick the quadrant (tutorial = learning path, how-to = specific task, explanation = background, reference = lookup).
2. Create the file under `docs/<quadrant>/`.
3. Add front matter with the correct `labels` value.
4. Link it from `docs/<quadrant>.md`.
5. Link it from root `README.md` if it's user-facing.

Full guide, including the Confluence publishing command for this doc set: `docs/internal/documentation-guide.md`.
