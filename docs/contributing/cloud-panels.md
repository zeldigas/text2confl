# Confluence Cloud Panel / Admonition Storage Format

This document describes the storage formats for panel-like blocks observed in Confluence Cloud (editor v2), and
catalogues the gaps between what text2confl currently emits and what the Cloud editor produces.

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

The `tip`, `info`, and `warning` callout types are stored as structured macros on Cloud v2. The macro name directly
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

| Admonition / panel type | Storage format                                                         |
|-------------------------|------------------------------------------------------------------------|
| `note` (Cloud v2)       | `ac:adf-extension` / `ac:adf-node type="panel"` with `panel-type=note` |
| `tip`                   | `ac:structured-macro ac:name="tip"`                                    |
| `info`                  | `ac:structured-macro ac:name="info"`                                   |
| `warning`               | `ac:structured-macro ac:name="warning"`                                |
| Custom colour + icon    | `ac:structured-macro ac:name="panel"`                                  |

---

## Current implementation discrepancies

### AsciiDoc — `block_admonition.html.slim`

**File:** `convert/src/main/resources/com/github/zeldigas/text2confl/asciidoc/block_admonition.html.slim`

The template maps AsciiDoc admonition types to Confluence macro names, but several mappings are wrong, and the output
format for `note` is incorrect for Cloud v2:

| AsciiDoc type | Current output                            | Expected output (Cloud v2)                |
|---------------|-------------------------------------------|-------------------------------------------|
| `note`        | `ac:structured-macro ac:name="info"`      | `ac:adf-extension` with `panel-type=note` |
| `tip`         | `ac:structured-macro ac:name="tip"` ✓     | —                                         |
| `caution`     | `ac:structured-macro ac:name="note"`      | `ac:structured-macro ac:name="warning"`   |
| `warning`     | `ac:structured-macro ac:name="warning"` ✓ | —                                         |
| `important`   | `ac:structured-macro ac:name="warning"` ✓ | —                                         |

The `note → info` substitution is doubly wrong on Cloud v2: it produces the wrong macro name *and* the wrong format
(should be `ac:adf-extension`). The `editor_version()` helper already exists in `helpers.rb` (line 278) and returns
`'v2'` for Cloud editor v2, but `block_admonition.html.slim` never calls it.

The template also emits `<ac:parameter ac:name="title">` when a title is present, but this parameter is not supported on
Cloud — the title is silently ignored. When a title is provided it should instead be rendered as bold text on the first
line of the macro body.

The template also has no support for the custom `panel` macro with colour/icon parameters.

### Markdown — `ConfluenceNodeRenderer#render(AdmonitionBlock, …)`

**File:**
`convert/src/main/kotlin/com/github/zeldigas/text2confl/convert/markdown/ConfluenceStorageFormatConverters.kt`,
`render(AdmonitionBlock, …)` at line 508

1. **`note` emits the wrong format on Cloud v2.** The renderer always calls
   `html.openTag("ac:structured-macro", mapOf("ac:name" to "note"))`. On Cloud v2 it should instead emit
   `ac:adf-extension` with `panel-type=note`. The `cloudEditorV2()` helper (line 295) already exists on the class but is
   not consulted here.

2. **Admonition type set is too narrow.** `ADMONITION_TYPES` (line 117) is
   `{"tip", "note", "info", "warning", "expand"}`. Any type outside this set silently falls back to `"note"`. There is
   no mapping for `caution` or `important`, and no support for the custom `panel` macro.

3. **`title` parameter is emitted but unsupported on Cloud.** When `AdmonitionBlock` has a title, the renderer calls
   `html.addParameter("title", …)`. On Cloud this parameter is ignored. Instead, the title should be injected as bold
   text at the start of the `ac:rich-text-body`.

4. **`expand` is a category error.** `expand` is a collapsible-section macro, not a callout. It should not be in
   `ADMONITION_TYPES`; it needs a separate rendering path.

5. **No support for the custom `panel` macro.** There is no way to produce an `ac:structured-macro ac:name="panel"` with
   `bgColor` / `panelIcon` parameters from Markdown source.
