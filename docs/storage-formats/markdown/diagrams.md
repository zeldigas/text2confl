---
labels: supported-format,markdown
---

# Markdown - diagrams

**text2confl** supports a number of text diagram formats to be embedded in confluence page as code blocks:

* [PlantUML](https://plantuml.com/en/)
* [Mermaid](https://mermaid.js.org/)

[//]: # (* [Kroki]&#40;https://kroki.io/&#41;)

By default, **text2confl** provides zero-configuration support lookup for every type of supported diagram is detected in
runtime during startup. If detection failed to verify that external tool is present then support is turned off and code
blocks remains simple code blocks.

Generated diagrams are attached to page like a regular files.

## Name generation

As every diagram translated to separate page attachment, there are 2 options to control its name:

* explicitly specify it using `target` code block attribute. This is recommended approach, because it provides
  consistent naming
  when changing diagram content
* use automatically generated names based on code block content. In this case attachment name is equal to sha256 hash of
  content.

## Location where diagrams are generated

By default, generated diagrams are saved in `.diagrams` directory under documents root. 

This is configurable with the following parameters in `.text2confl.yml` file

```yaml
markdown:
  diagrams:
#  parameters here
```

| name       | description                                                                                       | default value |
|------------|---------------------------------------------------------------------------------------------------|---------------|
| `base-dir` | Base directory to store diagrams. Relative path is resolved from directory with `.text2confl.yml` | `.diagrams`   |
| `temp-dir` | Use random temporary directory instead of `base-dir`                                              | `false`       |


## Formats

### PlantUML

Code block language tags: `plantuml`, `puml`

```puml
!theme carbon-gray
Bob->Alice : Hello!
```

{target=puml-sample}

PlantUML is bundled in **text2confl** Docker image.

Supported code-block attributes:

* `target` - base name for generated image _without extension_
* `format` - what format to use for this diagram. Allowed values - `png`, `svg`
* `theme` - plantuml theme to use for rendering. Passed as `-theme` cli option

#### Generator configuration

PlantUML parameters can be specified in `.text2confl.yml` file:

```yaml {title=.text2confl.yml}
markdown:
  diagrams:
    plantuml:
#      parameters here
```

| name             | description                                                                                                                                                                                                                                     | default value |
|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| `enabled`        | Enable PlantUML diagrams support                                                                                                                                                                                                                | `true`        |
| `executable`     | Command name to invoke for plantuml. There is no support for invoking a jar with `java -jar`, so you need to have a wrapper script that will do it and pass all arguments down. Relative path is resolved from directory with `.text2confl.yml` | `platuml`     |
| `default-format` | Format to use for generated images. Available options: `svg`, `png`                                                                                                                                                                             | `png`         |

### Mermaid

Code block language tags: `mermaid`

```mermaid
graph TD;
    A-->B;
    A-->C;
    B-->D;
    C-->D;
```

{target=mermaid-sample}

Mermaid is not bundled with **text2confl** Docker image by default, so you can create derivative image with it or use
non-docker version with locally installed mermaid-cli.

Supported code-block attributes:

* `target` - base name for generated image _without extension_
* `format` - what format to use for this diagram. Allowed values - `png`, `svg`

!!! warning

    mermaid generates non-reprodicible svg files, they contains unique identifer that is always different.
    This will force attachment to be always reuploaded

#### Generator configuration

Mermaid parameters can be specified in `.text2confl.yml` file:

```yaml {title=.text2confl.yml}
markdown:
  diagrams:
    mermaid:
#      parameters here
```

| name               | description                                                                                                                      | default value                                |
|--------------------|----------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------|
| `enabled`          | Enable Mermaid diagrams support                                                                                                  | `true`                                       |
| `executable`       | Command name to invoke for mermaid. Relative path is resolved from directory with `.text2confl.yml`                              | `mmdc`                                       |
| `default-format`   | Format to use for generated images. Available options: `svg`, `png`                                                              | `png`                                        |
| `config-file`      | Mermaid configuration file to pass for every diagram invocation. Relative path is resolved from directory with `.text2confl.yml` |                                              |
| `css-file`         | Mermaid css file to pass for every diagram. Relative path is resolved from directory with `.text2confl.yml`                      |                                              |
| `puppeeter-config` | Mermaid css file to pass for every diagram. Relative path is resolved from directory with `.text2confl.yml`                      | Value of `T2C_PUPPEETER_CONFIG` env variable |

