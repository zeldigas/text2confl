## Simple tables

You can use tables based on [GitHub Flavored Format](https://github.github.com/gfm/#tables-extension-):

| foo | bar |
|-----|-----|
| baz | bim |

## Complex tables

For complex cases (e.g. when you need multiline cells or complex content) you can just regular tables (`<table>...</table>`):

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

**Confluence Cloud** allows to specify any color by adding attribute `data-highlight-colour="#ffbbcc"` to cell, where `ffbbcc` is desired color.

**Confluence Server** only support preselected list of cell backgrounds and in addition to `data-highlight-colour` attribute you need to put `class="highlight-#c0b6f2"`, where `c0b6f2` is one of predefined colors.

Supported color codes in Server 7.x:

| color code |   description    |
|------------|------------------|
| `bf2600`   | Dark red 100%    |
| `ff8b00`   | Dark orange 100% |

<table>
<tbody>
<tr>
<th>Docs</th>
<td>

[reference](https://docs.spring.io/spring-kafka/reference/html)
</td>
</tr>
<tr>
<th>Samples</th>
<td>

[sample](http://example.org/sample)
</td>
</tr>
</tbody>
</table>

| First | Second |
|-------|--------|
| a     | a      |
| b     | b      |

