---
labels: reference
---

# Controlling images representation

The following attributes control how images are displayed in Confluence:

| Attribute   | Description                                                                                                                                          |
|-------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| align       | Controls image alignment on the page. Valid values: `left` (default), `right`, `center`                                                              |
| border      | Shows a border around the image. `true` or `false` (default).                                                                                        |
| title       | Sets the tooltip text displayed when hovering over the image. When the source format has a native way to specify a title, this attribute is ignored. |
| alt         | Sets the `alt` attribute on the rendered image. When the source format has a native way to specify alt text, this attribute is ignored.              |
| thumbnail   | Inserts a thumbnail instead of the full image. Useful for large images where you want a small preview by default.                                    |
| height      | Sets the image height in pixels.                                                                                                                     |
| width       | Sets the image width in pixels.                                                                                                                      |
| vspace      | Sets the amount of whitespace above and below the image in pixels.                                                                                   |
| hspace      | Sets the amount of whitespace to the left and right of the image in pixels.                                                                          |
| queryparams | Adds additional query parameters to the image URL. Used for image effects in Confluence Server.                                                      |
| class       | Adds a CSS class to the image element.                                                                                                               |
| style       | Adds inline CSS styling to the image element.                                                                                                        |
