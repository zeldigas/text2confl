# text2confl

<!-- Keep badges minimal and actionable. -->
[![Docker Image Version](https://img.shields.io/docker/v/zeldigas/text2confl?label=docker&sort=semver)](https://hub.docker.com/r/zeldigas/text2confl)
[![Docker Image Size](https://img.shields.io/docker/image-size/zeldigas/text2confl?label=image%20size&sort=semver)](https://hub.docker.com/r/zeldigas/text2confl)
[![Latest Release](https://img.shields.io/github/v/release/zeldigas/text2confl?display_name=tag&sort=semver)](https://github.com/zeldigas/text2confl/releases/latest)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)
[![CI](https://github.com/zeldigas/text2confl/actions/workflows/build.yml/badge.svg)](https://github.com/zeldigas/text2confl/actions/workflows/build.yml)

Publish docs written in structured text formats (Markdown, AsciiDoc) to Confluence (Server/DC and Cloud).

## If this project helps you

Please :star: **star this repo** — it helps other people discover the tool and motivates further maintenance.

## Why text2confl

`text2confl` focuses on turning docs-as-code (or any structured text) into a Confluence page tree:

- Works with **Confluence Cloud** and **Confluence Server/Data Center**
- Supports **Markdown** and **AsciiDoc** (and lets you mix formats in one documentation set)
- Tries to provide **good feature coverage** for the chosen markup language, mapped to Confluence capabilities
- Supports **extensibility/customization** (e.g., adding macros) so you don’t need to fork for common needs

## Quick start

If you want to quickly see how `text2confl` works, you can upload the documentation of this repository to your own Confluence.
Pick a space and a parent page under which the pages will be created:

```shell
# from the root of the checked out text2confl repo
text2confl upload --docs ./docs \
  --confluence-url https://wiki.example.org \
  --user bob \
  --password secret_password \
  --space DOCS \
  --parent "Text2Confl test"
```

Example resulting page tree:

![](docs/text2confl-page-tree.png)

**Note on a public docs showcase:** At the moment there is no public Confluence demo because Confluence Cloud free tier
doesn’t allow publishing spaces/pages to non-members. If you’d like to sponsor a public demo space or provide access for
a read-only showcase, please open an issue.

## Documentation

Start here:

- **User guide:** :book: [User guide](docs/user-guide.md)
- **Configuration reference:** :gear: [Configuration reference](docs/configuration-reference.md)

Deep dives / feature docs:

- **Storage formats:**
  - :mag: [Overview](docs/storage-formats.md)
  - :memo: [Markdown](docs/storage-formats/markdown.md)
  - :scroll: [AsciiDoc](docs/storage-formats/asciidoc.adoc)
- **Working with attributes:**
  - :page_facing_up: [Page attributes](docs/user-guide/page-attributes.md)
  - :framed_picture: [Image attributes](docs/user-guide/image-attributes.md)
  - :pushpin: [Table of contents (ToC) attributes](docs/user-guide/toc-attributes.md)
- **Content tweaks:**
  - :wrench: [Auto-fix content](docs/user-guide/auto-fix-content.md)
  - :computer: [Code blocks](docs/user-guide/code-blocks.md)
  - :art: [Table colors](docs/user-guide/table-colors.md)
  - :straight_ruler: [Table width](docs/contribution/table-width.md)
- **Advanced usage:**
  - :file_folder: [Virtual pages](docs/user-guide/virtual-pages.md)
  - :office: [Multitenant setups](docs/user-guide/multitenant.md)
  - :broom: [Pages cleanup](docs/user-guide/pages-cleanup.md)

Contributing:

- :handshake: [Contributing guide](CONTRIBUTING.md)
- :construction: [Contribution notes](docs/contribution.md)

## Who’s using text2confl

If your team uses `text2confl`, consider adding your company/project name here. It helps show that the tool is used in real setups.

- _Your company / project_

To get listed, open a PR that adds a bullet to this section. If you prefer not to do that publicly, you can also open an issue/PM to me and I’ll add it for you.

## Scope

`text2confl` does not assume the purpose of the publishing process or the type of content you are going to upload.
It can be formal docs managed as code, a to-do list, or a report generated from Jira.

Because of that there isn’t a strict “final” feature set. If you find something missing, please create an issue and
describe your needs.

## Design and usability goals

Key principles the tool tries to follow:

1. Provide good feature coverage for every supported source format. If you pick AsciiDoc, it should feel comfortable,
   not like you constantly hit unsupported features.

   _Note:_ features still need to map to Confluence formatting or macros.
2. Provide reasonable defaults and auto-detection where possible (e.g., Cloud vs Server editor differences).
3. Support both Confluence Server/Data Center and Cloud and be aware of differences in editors/features.
4. If the source format supports extensibility/customization, support it. There should be no need to fork just to add
   common macros.

## Supported source formats

- ✅ Markdown
- ✅ AsciiDoc
