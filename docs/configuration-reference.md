# Configuration reference

On this page you can find information about configuration options available in `.text2confl.yml` file as well as their
alternatives in command line or env variables format.

## Configuration options

!!! warning

     Keep in mind the lookup order of values:
        
     1. command line argument
     2. environment variable
     3. value in `.text2confl.yml`

| .text2confl.yml         | cli option                                 | env variable       | description                                                                                                                                                                                                                                                                         |
|-------------------------|--------------------------------------------|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| server                  | `--confluence-url`                         | `CONFLUENCE_URL`   | Address of confluence server. For Confluence Cloud specify in format `https://YOURSITE.atlassian.net/wiki`                                                                                                                                                                          |
| space                   | `--space`                                  | `CONFLUENCE_SPACE` | Space where documents will be uploaded. It is not possible to upload docs to multiple spaces in one run                                                                                                                                                                             |
| default-parent-id       | `--parent-id`                              |                    | Id of parent page where pages will be added as children if they don't have their own parent defined (either by attributes or by documents hierarchy). This parameter takes precedence over `default-parent`                                                                         |
| default-parent          | `--parent`                                 |                    | Title of parent page where pages will be added as children if they don't have their own parent defined (either by attributes or by documents hierarchy).                                                                                                                            |
| remove-orphans          | `--remove-orphans`                         |                    | Whether orphaned pages need to be cleaned up in some way. By default all "managed" pages are removed. You can find more about this on [dedicated page](user-guide/pages-cleanup.md)                                                                                                 |
| notify-watchers         | `--notify-watchers`/`--no-notify-watchers` |                    | Send notification about updated pages. In configuration file can be set to `true` or `false`                                                                                                                                                                                        |  
| title-prefix            |                                            |                    | Prefix that is appended to all pages in document tree. E.g. if title is "My page" and configured prefix is "(autodocs) " then resulting page title will be "(autodocs) My page"                                                                                                     | 
| title-postfix           |                                            |                    | Postfix that is appended to all pages in document tree. E.g. if title is "My page" and configured postfix is " (autodocs)" then resulting page title will be "My page (autodocs)"                                                                                                   | 
| editor-version          | `--editor-version`                         |                    | (For Confluence Cloud only). Editor version that is used for page rendering. It affects how page are displayed and some features. Autodetected by default so specify it only if for some reason you want to publish pages using `v1` editor in Cloud. Allowed values are `v1`, `v2` | 
| modification-check      | `--check-modification`                     |                    | Strategy to detect page changes. Allowed values: `hash` (default) and `content`. You can read more about their difference [below](#modifications-check-strategies).                                                                                                                 | 
| docs-location           |                                            |                    | Option to specify url where docs source root is located. If specified automatically enables appending of autogenerated note.                                                                                                                                                        | 
| add-autogenerated-note  |                                            |                    | Flag that can explicitly control if autogenerate note on top of page will be appended or not. Valid values are `true` and `false`                                                                                                                                                   | 
| autogenerated-note      |                                            |                    | Allow you to specify custom text that will be appended in *note* block on top of the page. Text should be in ***confluence storage format**. Default value can be found [below](#autogenerated-note)                                                                                |
| markdown                |                                            |                    | Section with markdown specific parameters                                                                                                                                                                                                                                           | 
| markdown.any-macro      |                                            |                    | Flag that specifies if any macro [will be rendered][simple_macros] is rendered or not. If not specified explicitly, value depends on `markdown.enabled-macros` parameter - `true` if it is empty and `false` otherwise                                                              |
| markdown.enabled-macros |                                            |                    | List that specifies what macros [will be rendered][simple_macros]. Empty by default.                                                                                                                                                                                                |

### Modifications check strategies

Text2confl tries to avoid unnecessary uploads of pages if there are no changes for them. Right now there are 2
strategies for change detection:

1. `hash` - is based on generating checksum (hashsum) of page body and comparing it to server page metadata. This is
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
2. `__file__` - will be replaced by path to file from document root (directory where `.text2confl.yml` is located)

## Additional options for `upload` command

| cli option                                             | env variable              | description                                                                                   |
|--------------------------------------------------------|---------------------------|-----------------------------------------------------------------------------------------------|
| `--message`                                            |                           | Message that will be added as change comment to every modified page                           |
| `--user`                                               | `CONFLUENCE_USER`         | Username to use for login in confluence                                                       |
| `--password`                                           | `CONFLUENCE_PASSWORD`     | Password (or Cloud access token) to use for login in confluence                               |
| `--access-token`                                       | `CONFLUENCE_ACCESS_TOKEN` | Personal access token to use for loging in confluence. It is alternative to username/password |
| `--skip-ssl-verification`/`--no-skip-ssl-verification` |                           | Whether to ignore invalid ssl certificate when connecting to server                           |
| `--dry`/`--no-dry`                                     |                           | Activate (or explicitly deactivate) dry run mode                                              |


[simple_macros]: storage-formats/markdown.md#confluence-macros-with-simple-options