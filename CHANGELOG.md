# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

### Added

- Confluence Cloud Rest API v2 support. When uploading to cloud, it is enabled automatically. If you still need to use
  old approach (only legacy api), pass `--no-confluence-cloud` flag. This flag is supported for `export-to-md` and
  `upload` commands
- Support for rate limiting http requests (#209). Rate limiting is supported in two ways:
  - You can specify rps rate for sending requests
  - Client also honors `Retry-After` header when it receives response with 429 or 503 status 

### Changed

- Improved reporting about confluence unexpected errors: now headers are included
- `skip-ssl` parameter in `text2confl.yml` file is moved to `client.skip-ssl`

### Fixed

- False reporting about labels update, order is ignored now (#272)

## 0.19.0 - 2025-11-17

### Added

- Support for auto-wrapping collapsible code blocks in expand macro. Enabled by default in Cloud. (#268)

## 0.18.0 - 2025-11-11

### Added

- `<details>` tag in Markdown converted to expand macro. Internal summary works as title for expand macro

### Fixed

- Dependency updates

## 0.17.4 - 2025-06-20

### Fixed

- Removed ruby warning about duplicate constant registration
- Updated plantuml to 1.2025.3 along with asciidoctor-diagram to 3.0.1

## 0.17.3 - 2025-06-19

### Fixed

- Rolled back to plantuml 1.2025.2 due to issues with
  1.2025.3 (https://github.com/asciidoctor/asciidoctor-diagram/issues/484)

## 0.17.2 - 2025-06-18

### Changed

- Password confirmation is no longer asked in interactive prompt
- Dependency updates
    - plantuml to 1.2025.3
    - other deps (kotlin, ktor)

### Fixed

- Incorrect handling of empty body for attachment removal request (#243)

## 0.17.1 - 2024-06-27

### Fixed

- Using attributes enclosed in quotes, but not jsons. Extra scenario handled (#176)

### Changed

- Dependency updates
    - asciidoctor-diagram to 2.3.1

## 0.17.0 - 2024-06-24

### Added

- Check for conflict of published page with pages parent (#142)
- Support for Kroki in asciidoc files via `kroki-extension` (#162)

### Changed

- dependency updates:
    - plantuml to 1.2024.5
    - other deps (kotlin, ktor, logback, asciidoctor)

### Fixed

- Handling of quotes in image titles and alt text (#166)
- Escaping url for block images (#183)
- Using attributes enclosed in quotes, but not jsons (#176)

## 0.16.0 - 2024-01-07

### Added

- \[export-to-md] now resolves user references for Confluence Server (#51)

### Fixed

- handling of links with spaces for both Markdown and AsciiDoc. Such links need to be specified urlencoded (
  e.g. `dir%20with%20space/my-doc.md` for file in `dir with space` directory) and now file is properly resolved. (#83)
- \[AsciiDoc] `xrefstyle` attribute is taken into account for references (#136)

## 0.15.1 - 2024-01-02

### Fixed

- Detection of duplicate titles in scanned files (#131)
- User macro in asciidoc files with email format (#135)

### Changed

- dependency updates:
    - using image with Java 21 to distribute text2confl
    - plantuml to 1.2023.13
    - other deps (kotlin, ktor, jackson)

## 0.15.0 - 2023-11-06

### Added

- in `upload` and `export-to-md` commands you can enable logging of http requests/responses and configure request
  timeout
- configuration file can be named as `text2confl.yml` or `text2confl.yaml` in addition to dot-prefixed
  names (`.text2confl.yml`, `.text2confl.yaml`).
- `-v` option can be passed to enable verbose logging. Repeat up to three times to get more details in logs.
- `upload` command now print well-formatted summary of uploaded pages

### Changed

- dependency updates:
    - migrated to `io.github.oshai:kotlin-logging-jvm`
    - plantuml to 1.2023.12

### Fixed

- Non-local links detection (may cause crash on Windows, #116, #96)
- `export-to-md` now always uses `/` as path separator for attachments
- Difference in page names causes page to be renamed first. This stabilize upload operation (#25) and also fixes
  inconsistent cleanup of pages that were renamed.

## 0.14.0 - 2023-09-20

### Added

- `json` is added to supported languages on Confluence server

### Fixed

- `.text2confl.yml` config is now properly loaded with consistent property names (kebab case) and case insensitive enum
  values

### Changed

- `cli` model is split into 2: `core` and `cli`. Contributed by @dgautier.
- dependency updates
    - Asciidoctor diagram to 2.2.13
    - plantuml to 1.2023.11

## 0.13.0 - 2023-08-28

### Added

- docker image is now built for arm64 in addition to amd64. To support this, image was switched from alpine to regular
  jre image of eclipse temurin
- \[AsciiDoc] Support for underlined text (#74)

### Fixed

- \[AsciiDoc] No line breaks inside paragraph (#73)

## 0.12.0 - 2023-08-26

### Added

- Files in Asciidoc format. Features (see
  details [in docs](https://github.com/zeldigas/text2confl/tree/master/docs/storage-formats/asciidoc.adoc)):
    - All basic rendering features of asciidoc
    - 3 bundled Confluence-specific macros: status, userlink and generic macro `confl_macro` that allows to render any
      simple macro
    - Support for registering additional ruby libs
    - Support for `asciidoctor-diagram`

### Changed

- Dependency updates:
    - Clikt 4.2.0
    - Kotlin 1.9.10

## 0.11.0 - 2023-07-16

### Added

- Support for large amount of attachments on page. In this case they returned paginated.

### Changed

- PlantUML updated to 1.2023.10
- Kotlin updated to 1.9.0
- Ktor updated to 2.3.2

## 0.10.1 - 2023-05-28

### Added

- docker label pointing to GitHub repo. This will let `renovatebot` to fetch changelog

### Fixed

- removed wiremock from final distribution
- \[export-to-md] now properly handles page link without special link name
- \[export-to-md] adjusted simple table detection to handle simple tables with header row in `tbody`

## 0.10.0 - 2023-05-28

### Added

* Support for multi-tenant content in one space
* \[Markdown] Support for attachments that are not referenced on page by link. This can help to use macros that expect
  attachment as parameter.
* `export-to-md` command, that exports requested confluence page to Markdown and downloads all attachments. Handy when
  you migrate handwritten pages to repository stored file.

### Changed

* \[Markdown] Attachment naming for link and image references - attachment will be named after reference name.

  Example: `[my attachment]: some/location/with/my.txt` will be uploaded as `my attachment`.
* PlantUML updated to 1.2023.8

## 0.9.0 - 2023-05-14

### Added

* \[Markdown] Support for quoted usernames - `@"username@example.org"`
* \[Markdown] Support for auto-links in file (enabled by default)
* \[Markdown] Support for typography (enabled by default for dots and dashes)
* \[Markdown] Support for tables parsing customizations
* Support to customize language mapping for code blocks

### Changed

* \[Markdown] column spans are enabled by default for tables. Can be disabled via configuration
* PlantUML updated to 1.2023.7

## 0.8.0 - 2023-01-15

### Added

* \[Markdown] Support for diagrams as code blocks when corresponding tool is present
    * PlantUML diagrams (`puml` or `plantuml`) when `planuml` command is present
    * Mermaid diagrams (`mermaid`) when `mmdc` command is present
    * [Kroki](https://kroki.io) diagrams
* Ability to set extra properties for pages (more in [docs](./docs/user-guide/page-attributes.md#page-properties)):
    * any page attribute with name `property_<property_name>`

### Changed

* Dockerfile bundles `plantuml` jar file so, plantuml diagrams can be generated out of the box when docker image is used
* Using `eclipse-temurin` as base docker image, as `openjdk` is deprecated
* Updated various kotlin and generic dependencies
* Http client now has dedicated user agent - `text2confl`
* Split single-page doc about Markdown into separate pages

### Fixed

* Errors in converted files like unbalanced tags produce non-zero error code

## 0.7.0 - 2022-11-27

0.11.0 - 2023-07-16

### Added

* Executable for Windows in distro
* Support for virtual pages: pages that are present in hierarchy but maintained manually. Read more
  in [user guide](docs/user-guide/virtual-pages.md)
* Fixing of pages parent even when content is not changed

## 0.6.0 - 2022-09-18

### Changed

* Updated dependencies: kotlin 1.7, ktor 2.1, latest jackson

### Added

* Added check that converted file is a valid xml before trying to upload content to server
* Support for emoji text-codes using `:metal:` formatting: :metal:

## 0.5.1 - 2022-04-02

### Fixed

* Symlink to command was missing in PATH of docker image

## 0.5.0 - 2022-04-02

### Added

* \[Markdown] Support for any Confluence macros with simple key-value parameters. Ref with
  format `[MACRONAME param1=value1]`
  will insert `macroname` macros with one parameter `param1` (#20).

## 0.4.2 - 2022-03-26

### Fixed

* Don't fail when modifying/deleting attachment in Confluence Server (#18)
* \[Markdown] User reference now supports `-` symbol in the middle of username, e.g. `~user-name` (#21)

### Changed

* Cleanup of orphaned pages is done in parallel to reduce execution time

## 0.4.1 - 2022-03-14

### Fixed

* Now labels from confluence server associated with page deserialized properly (#16)

## 0.4.0 - 2022-03-13

### Changed

* \[Markdown] Missing TOC properties are supported now: `outline`
* \[Markdown] TOC parameters can be specified in `[TOC]` block.

### Fixed

* Orphaned pages are now detected in root parents and leaf pages. Parent page is also properly tracked as now real
  parent is used instead of *default parent* (because every page can configure parent in attributes).

## 0.3.0 - 2022-03-07

### Added

* Ability to add autogenerated note block with parametrized text that by defaults point to source file location.
* `convert` command now takes into account parameters from `.text2confl.yml` file
* Support for orphaned child pages removal using 2 strategies:
    * `managed` (default) - to remove pages only created by `text2confl` itself. It is good to clear dangling pages
      after rename while allowing humans to maintain pages under generated ones
    * `all` - to remove all child pages. It is good if you want to enforce policy that all pages should be managed in
      code
* Support for dry run upload - no modifications will be done. Instead, potential actions will be logged with (dryrun)
  marker

### Fixed

* Updating page labels when page on server does not have any label

## 0.2.0 - 2022-02-12

### Added

* Ability to override parent via page attributes. This can be handy for use-cases when root pages do not belong to
  single parent
* \[Markdown] Support for status macro via custom html tag: `<status color="red">STATUS text</status>`
* \[Markdown] Support for confluence username reference (`@username`)
* \[Markdown] Support for ^superscript^ text

### Fixed

* \[Confluence Cloud] Creation of page - turns out cloud version sets editor property on page creation, so call to
  create this property fails
* \[Confluence Cloud] Fixed line soft wraps. Editor v2 does paragraph breaks on soft wraps that is undesirable
* Space details resolution used hardcoded space key, now provided key is used properly

## 0.1.0 - 2022-02-06

Initial release of application.

### Added

* Uploading to confluence with username/password and access tokens
* Parallel upload of pages to reduce time
* Support for editor v1 and v2. Uploaded page is marked accordingly
* Support for files in Markdown format with the following features specific to Confluence
    * Table of Contents
    * Task lists
    * Admonitions (tip, note, warning, info blocks)
    * Attachments
