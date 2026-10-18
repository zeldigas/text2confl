# Confluence Cloud Panel / Admonition Storage Format

This document describes the storage formats for panel-like blocks observed in Confluence Cloud (editor v2), and
how text2confl maps admonitions to them.

The sample markup in this document was captured from a page created and saved with the Confluence Cloud v2 editor.

---

## Formats in use on Confluence Cloud

### 1. ADF panel (`ac:adf-extension`)

The Cloud v2 editor stores the **"note" admonition type** using the ADF extension format — a purple/lavender box
Confluence considers the native "note" panel. The other named callout types (tip, info, warning) still use
`ac:structured-macro`.

```xml

<ac:adf-extension>
    <ac:adf-node type="panel">
        <ac:adf-attribute key="panel-type">note</ac:adf-attribute>
        <ac:adf-attribute key="local-id">d06bb716804d</ac:adf-attribute>
        <ac:adf-content>
            <p local-id="b2974c8b086f">Panel body content.</p>
        </ac:adf-content>
    </ac:adf-node>
    <ac:adf-fallback>
        <div class="panel conf-macro output-block"
             style="background-color: rgb(234,230,255);border-color: rgb(153,141,217);border-width: 1.0px;">
            <div class="panelContent" style="background-color: rgb(234,230,255);">
                <p local-id="b2974c8b086f">Panel body content.</p>
            </div>
        </div>
    </ac:adf-fallback>
</ac:adf-extension>
```

**Attributes on `ac:adf-node`:**

| Attribute key | Description                                                                 |
|---------------|-----------------------------------------------------------------------------|
| `panel-type`  | `note` — the only value observed for this format                            |
| `local-id`    | Stable short hex DOM identifier, e.g. `d06bb716804d`, omitted by text2confl |

`ac:adf-fallback` is required and contains HTML that non-ADF viewers render instead.

### 2. Named callout macros (`ac:structured-macro`)

The `tip`, `info`, `note` and `warning` callout types are stored as structured macros on Cloud v2. The macro name directly
determines the visual style.

```xml

<ac:structured-macro ac:name="tip" ac:schema-version="1"
                     ac:local-id="e36aa6fbf740"
                     ac:macro-id="795e6134-6d0c-4b45-9e55-07ed2c2fa4c9">
    <ac:rich-text-body>
        <p>Content here.</p>
    </ac:rich-text-body>
</ac:structured-macro>
```

Valid `ac:name` values and their appearance:

| `ac:name` | Appearance           |
|-----------|----------------------|
| `tip`     | Green tip box        |
| `info`    | Blue info box        |
| `note`    | Yellow warning box   |
| `warning` | Red/orange error box |

> **Note:** The `title` parameter (`<ac:parameter ac:name="title">`) is **not supported** by these macros on Confluence
> Cloud. Passing it has no effect.

### 3. Custom panel macro (`ac:structured-macro ac:name="panel"`)

A structured macro for panels with a fully custom background colour and optional emoji icon. Also stored as a structured
macro on Cloud v2.

```xml

<ac:structured-macro ac:name="panel" ac:schema-version="1"
                     ac:local-id="d0d3d79e14ee"
                     ac:macro-id="c1e672ae-cef3-40f6-8a97-255a650cb589">
    <ac:parameter ac:name="panelIcon">:rainbow:</ac:parameter>
    <ac:parameter ac:name="panelIconId">1f308</ac:parameter>
    <ac:parameter ac:name="panelIconText">🌈</ac:parameter>
    <ac:parameter ac:name="bgColor">#E6FCFF</ac:parameter>
    <ac:rich-text-body>
        <p>Custom panel content.</p>
    </ac:rich-text-body>
</ac:structured-macro>
```

**Parameters:**

| Parameter       | Description                            |
|-----------------|----------------------------------------|
| `panelIcon`     | Emoji shortcode, e.g. `:rainbow:`      |
| `panelIconId`   | Unicode code point (hex), e.g. `1f308` |
| `panelIconText` | Rendered emoji character, e.g. `🌈`    |
| `bgColor`       | Background hex colour, e.g. `#E6FCFF`  |
| `title`         | Optional visible panel title           |

---

## Summary: which format for which type

| Cloud panel (UI name) | Storage format                                                         |
|-----------------------|------------------------------------------------------------------------|
| Info                  | `ac:structured-macro ac:name="info"`                                   |
| Success               | `ac:structured-macro ac:name="tip"`                                    |
| Note (lavender)       | `ac:adf-extension` / `ac:adf-node type="panel"` with `panel-type=note` |
| Warning               | `ac:structured-macro ac:name="note"`                                   |
| Error                 | `ac:structured-macro ac:name="warning"`                                |
| Custom colour + icon  | `ac:structured-macro ac:name="panel"`                                  |

---

## Current mapping in text2confl

Admonition types are mapped to panels by color scheme (#347):

| Source                                                         | Server/DC macro | Cloud v2 panel      |
|----------------------------------------------------------------|-----------------|---------------------|
| GitHub `[!NOTE]`, AsciiDoc `NOTE`, `!!! info`                  | `info`          | Info                |
| GitHub `[!TIP]`, AsciiDoc `TIP`, `!!! tip`                     | `tip`           | Success             |
| GitHub `[!IMPORTANT]`, AsciiDoc `IMPORTANT`, `!!! important`   | `note`          | Note (ADF panel)    |
| GitHub `[!WARNING]`, AsciiDoc `CAUTION`, `!!! note`            | `note`          | Warning             |
| GitHub `[!CAUTION]`, AsciiDoc `WARNING`, `!!! warning`         | `warning`       | Error               |

Implementation:

- AsciiDoc: `block_admonition.html.slim` with type mapping in `map_to_confluence_admonition_type` (`helpers.rb`)
- Markdown: `ConfluenceNodeRenderer#render(AdmonitionBlock, …)` in `ConfluenceStorageFormatConverters.kt`, GitHub
  alert types are mapped in `GhAdmonitionExtension.kt`
- Export to Markdown: ADF `note` panel is exported as `!!! important` (`ConfluenceCustomNodeRenderer.kt`)

On Cloud v2 admonition title is rendered as bold first paragraph of panel body instead of `title` parameter. The
autogenerated note is rendered as ADF note panel on Cloud v2.

Not supported yet: custom `panel` macro with colour/icon parameters (neither in AsciiDoc nor in Markdown).
