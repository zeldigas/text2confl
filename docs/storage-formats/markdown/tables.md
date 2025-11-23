---
labels: supported-format,markdown
---

# Markdown - tables

## Simple tables

You can use tables based on [GitHub Flavored Format](https://github.github.com/gfm/#tables-extension-):

Markdown table:

```markdown
| foo                 |     bar      |               baz | 
|---------------------|:------------:|------------------:|
| bip                 |     bim      |              boop |
| I'm left by default | I'm centered | I'm aligned right |
```

Will produce:

| foo                 |     bar      |               baz | 
|---------------------|:------------:|------------------:|
| bip                 |     bim      |              boop | 
| I'm left by default | I'm centered | I'm aligned right | 

### Table width

To set table width, you can use _attributes_ under the table. Provided value is treated as width in percent, `%` is optional:

```markdown
| foo | bar |  baz | 
|-----|:---:|-----:|
| bip | bim | boop |
{width=75%}
```

| foo | bar |  baz | 
|-----|:---:|-----:|
| bip | bim | boop |
{width=75%}

## Complex tables

For complex cases (e.g. when you need multiline cells or complex content) you can just regular
tables (`<table>...</table>`):

Html table:

````markdown
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
````

Will produce:

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