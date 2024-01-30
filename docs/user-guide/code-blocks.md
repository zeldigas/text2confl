# Code highlight

**text2confl** is familiar with different list of supported language highlights in Confluence Server/DataCenter and
Cloud,
so it will keep language from language tag if Confluence will support it and remove it when such languages are not
supported. [Below](#language-tags) you will find list of supported highlights.

List of supported attributes:

| Attribute     | Description                                                                                                                       | Confluence Server   | Confluence Cloud                                        |
|---------------|-----------------------------------------------------------------------------------------------------------------------------------|---------------------|---------------------------------------------------------|
| `title`       | Title for code block                                                                                                              | :white_check_mark:️ | :white_check_mark:️                                     |
| `collapse`    | Flag that control if code block is collapsed on page by default. Allowed values - `true` or `false`                               | :white_check_mark:️ | :warning:️ Only if editor v1 is forcibly set :warning:️ |
| `linenumbers` | Flag that controls if line numbers are displayed. Allowed values - `true` or `false`                                              | :white_check_mark:️ | :warning:️ Only if editor v1 is forcibly set :warning:️ |
| `firstline`   | Sets starting line number for codeblocks                                                                                          | :white_check_mark:️ | :warning:️ Only if editor v1 is forcibly set :warning:️ |
| `theme`       | Sets theme for code block. Valid values - in [Confluence docs](https://confluence.atlassian.com/doc/code-block-macro-139390.html) | :white_check_mark:️ | :warning:️ Only if editor v1 is forcibly set :warning:️ |

## Language tags

### Confluence server

List of supported languages: `actionscript3`, `applescript`, `bash`, `c#`, `coldfusion`, `cpp`, `css`, `delphi`, `diff`,
`erl`, `groovy`, `java`, `jfx`, `js`, `perl`, `php`, `powershell`, `py`, `ruby`, `sass`, `scala`, `sql`, `text`, `vb`,
`xml`, `yml`.

Extra aliases for some languages are provided to ease compatibility with IDE or widely used language tags that are not
supported in Confluence:

| language tag | Confluence language |
|--------------|---------------------|
| `shell`      | `bash`              |
| `zsh`        | `bash`              |
| `sh`         | `bash`              |
| `dockerfile` | `bash`              |
| `javascript` | `js`                |
| `json`       | `js`                |
| `yaml`       | `yml`               |
| `html`       | `xml`               |

Any language not in list are mapped as `java`.

### Confluence cloud

List of supported languages: `abap`, `actionscript3`, `ada`, `applescript`, `arduino`, `autoit`, `bash`, `c`, `c#`,
`clojure`, `coffeescript`, `coldfusion`, `cpp`, `css`, `cuda`, `d`, `dart`, `diff`, `elixir`, `erl`, `fortran`,
`foxpro`, `go`, `graphql`, `groovy`, `haskell`, `haxe`, `html`, `java`, `javafx`, `js`, `json`, `jsx`, `julia`,
`kotlin`, `livescript`, `lua`, `mathematica`, `matlab`, `objective-c`, `objective-j`, `ocaml`, `octave`, `pas`,
`perl`, `php`, `powershell`, `prolog`, `puppet`, `py`, `qbs`, `r`, `racket`, `restructuredtext`, `ruby`, `rust`,
`sass`, `scala`, `scheme`, `smalltalk`, `splunk-spl`, `sql`, `standardlm`, `swift`, `tcl`, `tex`, `text`, `tsx`,
`typescript`, `vala`, `vb`, `vbnet`, `verilog`, `vhdl`, `xml`, `xquery`, `yaml`.

Extra aliases for some languages are provided to ease compatibility with IDE or widely used language tags that are not
supported in Confluence:

| language tag | Confluence language |
|--------------|---------------------|
| `shell`      | `bash`              |
| `zsh`        | `bash`              |
| `sh`         | `bash`              |
| `dockerfile` | `bash`              |
| `javascript` | `js`                |
| `yaml`       | `yml`               |

