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

It is possible to set background for cell via special attribute on cell: [Table cell coloring](../../user-guide/table-colors.md)