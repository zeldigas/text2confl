---
labels: contributing
---

# Table details in confluence storage format

## Confluence cloud format

Confluence Cloud has a completely different approach to table and column width handling.

On `table` level you need to define 2 attributes that control width and positioning:

1. `data-table-width` - contains integer value that represent width in virtual pixels
2. `data-layout` - controls how table is positioned on page. Supported values:
    - `default` - constant value for narrow page mode for default width (760px)
    - `center` - can be set to center table in wide page mode, also default for narrow page mode when table has custom
      width
    - `align-start` - left align. Default for wide page mode

There are 2 main width values - `1800` for table that takes full width and `760` for narrow mode.

Column widths are specified in regular `colgroup/col` elements but using exact values in pixels -
`<col style="width: 147.0px;" />`.

That gives us the following steps to identify parameters for table and columns:

1. Check width mode - wide or narrow - by checking page attribute `property-content-appearance-published`. If it is set
   to `full-width`, then we are in wide mode. If attribute is missing or has different value, we are in narrow mode.

   This will give us base width for table - either `1800` or `760` when no custom width is set.
2. If custom width is set on table, treat it as percentage of full width. Calculate actual width in pixels by
   multiplying base width
   with percentage value: `actual_width = 1800 * (custom_width / 100)`.
3. If format allows specifying column width, calculate actual width for every column as percentage of table width:
   `column_actual_width = table_actual_width * (column_width / 100)`
4. Calculate value for `data-layout` attribute.
    - If alignment is not customized, use proper value based on width mode:
        - narrow mode - `default` or `center` depending on whether table has custom width or not
        - wide mode - `align-start`
    - if alignment is customized, use `align-start` for left alignment and `center` for center alignment in wide mode.
      In narrow mode alignment is always `center`.

Based on the ratio between default widths in narrow and wide modes, the percentage value that sets the table width to
match its narrow-mode default is `42` (760 / 1800 * 100).
