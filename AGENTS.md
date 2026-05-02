# text2confl — Agent Guide

`text2confl` publishes structured text files (Markdown, AsciiDoc) to Confluence, maintaining page hierarchies and metadata.

## Tech Stack

**Language & runtime**
- Kotlin 2.3, JVM target Java 17
- Maven (multi-module build, 5 modules)

**Key libraries**
- Flexmark 0.64 — Markdown parsing and rendering
- AsciidoctorJ 3.0 — AsciiDoc processing
- Ktor 3.4 — async HTTP client
- Jackson 2.21 — JSON/YAML serialization
- Clikt 5.1 — CLI framework
- JUnit Jupiter, AssertK, MockK, WireMock — testing

**Module dependency chain**

```
cli
 └── core
      ├── convert
      └── confluence-client

gem-kroki  (standalone diagram plugin)
```

## Build and Test

Build everything from the project root:

```shell
mvn package
```

Individual modules are not installed to the local Maven repository, so use `-am` when targeting a specific module to also build its upstream dependencies:

```shell
# build a specific module
mvn package -pl convert -am

# test a specific module
mvn test -pl core -am

# test everything
mvn test
```

## Code Navigation

### CLI commands

| File | Description |
|------|-------------|
| `cli/src/main/kotlin/com/github/zeldigas/text2confl/cli/Main.kt` | Main entry point — `ConfluencePublisher` (Clikt root command), registers subcommands |
| `cli/src/main/kotlin/com/github/zeldigas/text2confl/cli/Upload.kt` | `upload` subcommand — converts and publishes docs to Confluence |
| `cli/src/main/kotlin/com/github/zeldigas/text2confl/cli/Convert.kt` | `convert` subcommand — converts source files to Confluence storage format and writes HTML output |

### Confluence client

| File | Description |
|------|-------------|
| `confluence-client/src/main/kotlin/com/github/zeldigas/confclient/ConfluenceClient.kt` | Interface — all supported Confluence REST API operations (pages, labels, attachments, properties) |
| `confluence-client/src/main/kotlin/com/github/zeldigas/confclient/ConfluenceClientImpl.kt` | Implementation — Ktor-based HTTP client, handles Cloud vs Server/DC differences |

### Markdown converter

| File | Description |
|------|-------------|
| `convert/src/main/kotlin/com/github/zeldigas/text2confl/convert/markdown/MarkdownFileConverter.kt` | Entry point — `convert(file, context)` drives the markdown-to-storage-format pipeline |
| `convert/src/main/kotlin/com/github/zeldigas/text2confl/convert/markdown/MarkdownParser.kt` | Flexmark parser setup — registers extensions for diagrams, tables, admonitions, macros, etc. |

### AsciiDoc converter

| File | Description |
|------|-------------|
| `convert/src/main/kotlin/com/github/zeldigas/text2confl/convert/asciidoc/AsciidocFileConverter.kt` | Entry point — `convert(file, context)` drives the asciidoc-to-storage-format pipeline |
| `convert/src/main/kotlin/com/github/zeldigas/text2confl/convert/asciidoc/AsciidocParser.kt` | AsciidoctorJ wrapper — loads custom templates and rendering options |

### Format dispatcher

| File | Description |
|------|-------------|
| `convert/src/main/kotlin/com/github/zeldigas/text2confl/convert/Converter.kt` | `UniversalConverter` — routes files to the correct converter by extension, recursively builds the page hierarchy from a directory tree |

### Upload orchestration

| File | Description |
|------|-------------|
| `core/src/main/kotlin/com/github/zeldigas/text2confl/core/upload/ContentUploader.kt` | Top-level orchestrator — `uploadPages()` drives the full upload loop: create/update pages, sync labels and attachments, detect orphans |
| `core/src/main/kotlin/com/github/zeldigas/text2confl/core/upload/PageUploadOperations.kt` | Interface for per-page operations |
| `core/src/main/kotlin/com/github/zeldigas/text2confl/core/upload/PageUploadOperationsImpl.kt` | Implementation — change detection (hash or content comparison), labels/attachments sync, orphan deletion |
| `core/src/main/kotlin/com/github/zeldigas/text2confl/core/ServiceProvider.kt` | Factory — wires converter, Confluence client, and uploader together from configuration |

## Documentation

Docs live in `docs/` and follow the **Diataxis** four-quadrant model (tutorials, how-to, explanation, reference). Both Markdown (`.md`) and AsciiDoc (`.adoc`) files coexist in the same docs set.

See [`docs/internal/documentation-guide.md`](docs/internal/documentation-guide.md) for the full documentation maintenance guide.
