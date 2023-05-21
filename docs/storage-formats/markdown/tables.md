---
labels: supported-format,markdown
---

# Markdown - tables

## Simple tables

You can use tables based on [GitHub Flavored Format](https://github.github.com/gfm/#tables-extension-):

| foo | bar |
|-----|-----|
| baz | bim |

## Complex tables

For complex cases (e.g. when you need multiline cells or complex content) you can just regular
tables (`<table>...</table>`):

<table>
<thead>
<tr>
<th>First column</th>
<th>Second column</th>
</tr>
</thead>
<tbody>
<tr>
<td class="highlight-#ffbdad" data-highlight-colour="#ffbdad">

```
code block
that you might need to put in table
```

</td>
<td data-highlight-colour="#eeffbb">Simple text cell</td>
</tr>
</tbody>
</table>

### Cell background

It is possible to set background for cell

**Confluence Cloud** allows to specify any color by adding attribute `data-highlight-colour="#ffbbcc"` to cell,
where `ffbbcc` is desired color.

**Confluence Server** only support preselected list of cell backgrounds and in addition to `data-highlight-colour`
attribute you need to put `class="highlight-#c0b6f2"`, where `c0b6f2` is one of predefined colors.

Supported color codes in Server 7.x:

| color code | description        |
|------------|--------------------|
| `bf2600`   | Dark red 100%      |
| `ff8b00`   | Dark orange 100%   |
| `006644`   | Dark green 100%    |
| `008da6`   | Dark teal 100%     |
| `0747a6`   | Dark blue 100%     |
| `403294`   | Dark purple 100%   |
| `000000`   | Black 100%         |
| `de350b`   | Dark red 85%       |
| `ff991f`   | Dark orange 85%    |
| `00875a`   | Dark green 85%     |
| `00a3bf`   | Dark teal 85%      |
| `0052cc`   | Dark blue 85%      |
| `5243aa`   | Dark purple 85%    |
| `172b4d`   | Dark grey 100%     |
| `ff5630`   | Medium red 100%    |
| `ffab00`   | Medium orange 100% |
| `36b37e`   | Medium green 100%  |
| `00b8d9`   | Medium teal 100%   |
| `0065ff`   | Medium blue 100%   |
| `6554c0`   | Medium purple 100% |
| `42526e`   | Medium grey 100%   |
| `ff7452`   | Medium red 85%     |
| `ffc400`   | Medium yellow 100% |
| `57d9a3`   | Medium green 65%   |
| `00c7e6`   | Medium teal 85%    |
| `2684ff`   | Medium blue 85%    |
| `8777d9`   | Medium purple 85%  |
| `7a869a`   | Medium grey 85%    |
| `ff8f73`   | Medium red 65%     |
| `ffe380`   | Medium yellow 45%  |
| `79f2c0`   | Medium green 45%   |
| `79e2f2`   | Medium teal 45%    |
| `4c9aff`   | Medium blue 65%    |
| `998dd9`   | Medium purple 65%  |
| `c1c7d0`   | Medium grey 45%    |
| `ffbdad`   | Light red 100%     |
| `fff0b3`   | Light yellow 100%  |
| `abf5d1`   | Light green 100%   |
| `b3f5ff`   | Light teal 100%    |
| `b3d4ff`   | Light blue 100%    |
| `c0b6f2`   | Light purple 100%  |
| `f4f5f7`   | Light grey 100%    |
| `ffebe6`   | Light red 35%      |
| `fffae6`   | Light yellow 35%   |
| `e3fcef`   | Light green 35%    |
| `e6fcff`   | Light teal 35%     |
| `deebff`   | Light blue 35%     |
| `eae6ff`   | Light purple 35%   |

