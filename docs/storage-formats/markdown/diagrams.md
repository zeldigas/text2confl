---
labels: supported-format,markdown
---

# Markdown - diagrams

**text2confl** supports a number of text diagram formats to be embedded in confluence page as code blocks:

* [PlantUML](https://plantuml.com/en/)
* [Mermaid](https://mermaid.js.org/)
* [Kroki](https://kroki.io/)

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

This is configurable with the following parameters in `text2confl.yml` file

```yaml
markdown:
  diagrams:
#  parameters here
```

| name       | description                                                                                      | default value |
|------------|--------------------------------------------------------------------------------------------------|---------------|
| `base-dir` | Base directory to store diagrams. Relative path is resolved from directory with `text2confl.yml` | `.diagrams`   |
| `temp-dir` | Use random temporary directory instead of `base-dir`                                             | `false`       |

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

PlantUML parameters can be specified in `text2confl.yml` file:

```yaml {title=text2confl.yml}
markdown:
  diagrams:
    plantuml:
#      parameters here
```

| name             | description                                                                                                                                                                                                                                    | default value |
|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| `enabled`        | Enable PlantUML diagrams support                                                                                                                                                                                                               | `true`        |
| `executable`     | Command name to invoke for plantuml. There is no support for invoking a jar with `java -jar`, so you need to have a wrapper script that will do it and pass all arguments down. Relative path is resolved from directory with `text2confl.yml` | `platuml`     |
| `default-format` | Format to use for generated images. Available options: `svg`, `png`                                                                                                                                                                            | `png`         |

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

Mermaid parameters can be specified in `text2confl.yml` file:

```yaml {title=text2confl.yml}
markdown:
  diagrams:
    mermaid:
#      parameters here
```

| name               | description                                                                                                                      | default value                                |
|--------------------|----------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------|
| `enabled`          | Enable Mermaid diagrams support                                                                                                  | `true`                                       |
| `executable`       | Command name to invoke for mermaid. Relative path is resolved from directory with `text2confl.yml`                              | `mmdc`                                       |
| `default-format`   | Format to use for generated images. Available options: `svg`, `png`                                                              | `png`                                        |
| `config-file`      | Mermaid configuration file to pass for every diagram invocation. Relative path is resolved from directory with `text2confl.yml` |                                              |
| `css-file`         | Mermaid css file to pass for every diagram. Relative path is resolved from directory with `text2confl.yml`                      |                                              |
| `puppeeter-config` | Mermaid css file to pass for every diagram. Relative path is resolved from directory with `text2confl.yml`                      | Value of `T2C_PUPPEETER_CONFIG` env variable |

### Kroki

Kroki provides supports for multiple diagrams as a web service. **text2confl** by default enables support for Kroki and
uses public service for generation - <https://kroki.io>.

Example [Erd](https://github.com/BurntSushi/erd) diagram:

```erd {target=kroki-erd}
[Person]
*name
height
weight
+birth_location_id

[Location]
*id
city
state
country

Person *--1 Location
```

Supported code-block attributes:

* `target` - base name for generated image _without extension_
* `format` - what format to use for this diagram. Allowed values - `png`, `svg`
* `option_<option_name>` - extra option that will be passed to kroki generation. All options can be found
  in [official docs](https://docs.kroki.io/kroki/setup/diagram-options/). For example `option_size=1000x1000` will pass
  option `size` with value `1000x1000`

#### Generator configuration

Kroki parameters can be specified in `text2confl.yml` file:

```yaml {title=text2confl.yml}
markdown:
  diagrams:
    kroki:
#      parameters here
```

| name             | description                                                                                           | default value      |
|------------------|-------------------------------------------------------------------------------------------------------|--------------------|
| `enabled`        | Enable Kroki diagrams support                                                                         | `true`             |
| `server`         | Kroki server                                                                                          | `https://kroki.io` |
| `default-format` | Format to use for generated images. Available options: `svg`, `png`. Some diagrams support only `svg` | `png`              |

#### Supported formats

Kroki supports [more than 20 diagram formats](https://kroki.io/#support), including `plantuml` and `mermaid`. Native PlantUML and Mermaid generator takes higher precedence, but if you disable them, these formats will be still supported by Kroki.

Supported diagrams:

| Language codes                                                        | Formats      | Diagram format origin                                        |
|-----------------------------------------------------------------------|--------------|--------------------------------------------------------------|
| `puml`, `plantuml`, `c4plantuml`                                      | `png`, `svg` | [PlantUML](https://plantuml.com/en/)                         | 
| `mermaid`                                                             | `png`, `svg` | [Mermaid](https://mermaid.js.org/)                           | 
| `blockdiag`, `seqdiag`, `actdiag`, `nwdiag`, `packetdiag`, `rackdiag` | `png`, `svg` | [Blockdiag](https://github.com/blockdiag)                    | 
| `umlet`                                                               | `png`, `svg` | [UMlet](https://github.com/umlet/umlet)                      |
| `graphviz`, `dot`                                                     | `png`, `svg` | [GraphViz](https://www.graphviz.org/)                        |
| `erd`                                                                 | `png`, `svg` | [Erd](https://github.com/BurntSushi/erd)                     |
| `svgbob`                                                              | `svg`        | [Svgbob](https://github.com/ivanceras/svgbob)                |
| `nomnoml`                                                             | `svg`        | [nomnoml](https://github.com/skanaar/nomnoml)                |
| `vega`, `vegalite`                                                    | `png`, `svg` | [Vega](https://github.com/vega/vega)                         |
| `wavedrom`                                                            | `svg`        | [Wavedrom](https://github.com/wavedrom/wavedrom)             |
| `bpmn`                                                                | `svg`        | [BPMN](https://github.com/bpmn-io/bpmn-js)                   |
| `bytefield`                                                           | `svg`        | [Bytefield](https://github.com/Deep-Symmetry/bytefield-svg/) | 
| `excalidraw`                                                          | `svg`        | [Excalidraw](https://github.com/excalidraw/excalidraw)       |
| `pikchr`                                                              | `svg`        | [Pikchr](https://github.com/drhsqlite/pikchr)                |
| `structurizr`                                                         | `png`, `svg` | [Structurizr](https://github.com/structurizr/dsl)            |
| `diagramsnet`                                                         | `png`, `svg` | [Diagrams.net](https://www.diagrams.net/)                    |
| `ditaa`                                                               | `png`, `svg` | [Ditaa](https://ditaa.sourceforge.net/)                      |
| `d2`                                                                  | `svg`        | [D2](https://d2lang.com/)                                    |

        