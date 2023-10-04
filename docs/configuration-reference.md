# Configuration reference

On this page you can find information about configuration options available in `text2confl.yml` file as well as their
alternatives in command line or env variables format.

## Configuration options

!!! warning

     Keep in mind the lookup order of values:
        
     1. command line argument
     2. environment variable
     3. value in `text2confl.yml`

| text2confl.yml              | cli option                                 | env variable       | description                                                                                                                                                                                                                                                                         |
|-----------------------------|--------------------------------------------|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| server                      | `--Confluence-url`                         | `CONFLUENCE_URL`   | Address of Confluence server. For Confluence Cloud specify in format `https://YOURSITE.atlassian.net/wiki`                                                                                                                                                                          |
| space                       | `--space`                                  | `CONFLUENCE_SPACE` | Space where documents will be uploaded. It is not possible to upload docs to multiple spaces in one run                                                                                                                                                                             |
| default-parent-id           | `--parent-id`                              |                    | Id of parent page where pages will be added as children if they don't have their own parent defined (either by attributes or by documents hierarchy). This parameter takes precedence over `default-parent`                                                                         |
| default-parent              | `--parent`                                 |                    | Title of parent page where pages will be added as children if they don't have their own parent defined (either by attributes or by documents hierarchy).                                                                                                                            |
| remove-orphans              | `--remove-orphans`                         |                    | Whether orphaned pages need to be cleaned up in some way. By default all "managed" pages are removed. You can find more about this on [dedicated page](user-guide/pages-cleanup.md)                                                                                                 |
| notify-watchers             | `--notify-watchers`/`--no-notify-watchers` |                    | Send notification about updated pages. In configuration file can be set to `true` or `false`                                                                                                                                                                                        |  
| title-prefix                |                                            |                    | Prefix that is appended to all pages in document tree. E.g. if title is "My page" and configured prefix is "(autodocs) " then resulting page title will be "(autodocs) My page"                                                                                                     | 
| title-postfix               |                                            |                    | Postfix that is appended to all pages in document tree. E.g. if title is "My page" and configured postfix is " (autodocs)" then resulting page title will be "My page (autodocs)"                                                                                                   | 
| editor-version              | `--editor-version`                         |                    | (For Confluence Cloud only). Editor version that is used for page rendering. It affects how page are displayed and some features. Autodetected by default so specify it only if for some reason you want to publish pages using `v1` editor in Cloud. Allowed values are `v1`, `v2` | 
| modification-check          | `--check-modification`                     |                    | Strategy to detect page changes. Allowed values: `hash` (default) and `content`. You can read more about their difference [below](#modifications-check-strategies).                                                                                                                 | 
| tenant                      | `--tenant`                                 |                    | Tenant id for uploaded pages. You can find more info on [page about multi-tenancy](./user-guide/multitenant.md).                                                                                                                                                                    | 
| docs-location               |                                            |                    | Option to specify url where docs source root is located. If specified automatically enables appending of autogenerated note.                                                                                                                                                        | 
| add-autogenerated-note      |                                            |                    | Flag that can explicitly control if autogenerate note on top of page will be appended or not. Valid values are `true` and `false`                                                                                                                                                   | 
| autogenerated-note          |                                            |                    | Allow you to specify custom text that will be appended in *note* block on top of the page. Text should be in ***Confluence storage format**. Default value can be found [below](#autogenerated-note)                                                                                |
| code-blocks                 |                                            |                    | Section with configuration of code blocks language highlight                                                                                                                                                                                                                        |
| code-blocks.default-language |                                            |                    | Default language to use for code blocks highlight. Empty by default, Confluence will select type.                                                                                                                                                                                   |
| code-blocks.extra-mapping   |                                            |                    | Mapping (key - values) for codeblocks language. Allows you to customize target highlight language. Key is a language tag in document and value - Confluence language tag.                                                                                                           |
| markdown                    |                                            |                    | Section with markdown specific parameters                                                                                                                                                                                                                                           | 

### Markdown configuration options

Markdown can be configured in `text2confl.yml` file, in `markdown` section.

Table contains available parameters. Dot (`.`) means that this is next level, e.g.

```yaml
markdown:
  tables:
    column-spans: false
```

| Parameter                            | Default value                                                                                | Description                                                                                          |
|--------------------------------------|----------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| emoji                                | `true`                                                                                       | Support for [emojis in markdown][md_emoji].                                                          |
| auto-links                           | `true`                                                                                       | Enables [auto links][flex_links] - conversion of plain links into active links.                      |
| tables.column-spans ->               | `true`                                                                                       | Controls [COLUMN_SPANS][flex_tables] parameter of tables parsing                                     |
| tables.discard-extra-columns         | `true`                                                                                       | Controls [DISCARD_EXTRA_COLUMNS][flex_links] parameter of tables parsing                             |
| tables.append-missing-columns        | `true`                                                                                       | Controls [APPEND_MISSING_COLUMNS][flex_links] parameter of tables parsing                            |
| tables.header-separator-column-match | `true`                                                                                       | Controls [HEADER_SEPARATOR_COLUMN_MATCH][flex_tables] parameter of tables parsing                    |
| typography.quotes                    | `false`                                                                                      | Controls conversion of `"`, `'`, `<<`, `>>` into [special html tags][flex_typography]                |
| typography.smarts                    | `true`                                                                                       | Controls conversion of `'`, `...`, `. . .`, `--`, `---` into [special html tags][flex_typography]    |
| diagrams                             | `true`                                                                                       | Section that controls diagram generation. You can find details in [Markdown - diagrams][md_diagrams] |
| any-macro                            | depends on `markdown.enabled-macros` parameter - `true` if it is empty and `false` otherwise | Flag that specifies if any macro [will be rendered][simple_macros] is rendered or not.               |
| enabled-macros                       | empty list                                                                                   | List that specifies what macros [will be rendered][simple_macros].                                   |

[flex_links]: https://github.com/vsch/flexmark-java/wiki/Extensions#autolink

[flex_tables]: https://github.com/vsch/flexmark-java/wiki/Tables-Extension#parsing-details

[flex_typography]: https://github.com/vsch/flexmark-java/wiki/Typographic-Extension#overview

[md_emoji]: ./storage-formats/markdown/basic.md#emojis-

[md_diagrams]: ./storage-formats/markdown/diagrams.md

### AsciiDoc configuration options

AsciiDoc can be configured in `text2confl.yml` file, in `asciidoc` section.

Table contains available parameters. Dot (`.`) means that this is next level, e.g.

```yaml
asciidoc:
  attributes:
    plantuml-format: png
```

| Parameter      | Default value | Description                                                                                                                                  |
|----------------|---------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| diagrams       | `Diagrams`    | How to generate diagrams. The only option now is `Diagrams` that activates `asciidoctor-diagrams`                                            |
| bundled-macros | `true`        | Enables [asciidoctor macros for confluence](./storage-formats/asciidoc/confluence-specific.adoc)                                             |
| attributes     | emtpy map     | Section where you can specify key-value pairs. All provided pairs will be passed to Asciidoc as attributes.                                  | 
| gems           | empty list    | Additional ruby gems to _require_. Mentioned gems must be on classpath, packaged as jar files (like `asciidoctor-diagram`)                   | 
| temp-dir       | false         | If temporary directory should be used to store all generated content                                                                         | 
| base-dir       | `.asciidoc`   | Working directory where all generated content is stored if `temp-dir` option is not enabled. Directory is resolved relative to document root | 

### Modifications check strategies

Text2confl tries to avoid unnecessary uploads of pages if there are no changes for them. Right now there are 2
strategies for change detection:

1. `hash` - is based on generating checksum (hash sum) of page body and comparing it to server page metadata. This is
   default approach, and it works good if you are sure that no human will come and manually edit your page, because
   during manual edit metadata is unchanged so edits done in online editor will not be tracked.
2. `content` - is based on direct comparison of content. Right now this option works in very naive way and does not
   ignore any autogenerated properties of macro blocks.

### Autogenerated note

By default, autogenerated note will be the following:

```html
Edit <a href=\"__doc-root____file__\">source file</a> instead of changing page in Confluence.
<span style=\"color: rgb(122,134,154); font-size: small;\">Page was generated from source with <a
        href=\"https://github.com/zeldigas/text2confl\">text2confl</a>.</span>
```

You can specify your own text if you prefer it to be different. Note supports 2 parameters:

1. `__doc-root__` - will be replaced by value of `docs-location` option from config
2. `__file__` - will be replaced by path to file from document root (directory where `text2confl.yml` is located)

## Additional options for `upload` command

| cli option                                             | env variable              | description                                                                         |
|--------------------------------------------------------|---------------------------|-------------------------------------------------------------------------------------|
| `--message`                                            |                           | Message that will be added as change comment to every modified page                 |
| `--user`                                               | `CONFLUENCE_USER`         | Username to use for login in Confluence                                             |
| `--password`                                           | `CONFLUENCE_PASSWORD`     | Password (or Cloud access token) to use for login in Confluence                     |
| `--access-token`                                       | `CONFLUENCE_ACCESS_TOKEN` | Personal access token to use for Confluence. It is alternative to username/password |
| `--skip-ssl-verification`/`--no-skip-ssl-verification` |                           | Whether to ignore invalid ssl certificate when connecting to server                 |
| `--dry`/`--no-dry`                                     |                           | Activate (or explicitly deactivate) dry run mode                                    |

[simple_macros]: storage-formats/markdown/confluence-specific.md#confluence-macros-with-simple-options