# Storage formats

Markdown and AsciiDoc are supported.

## Markdown

> Markdown is a lightweight markup language for creating formatted text using a plain-text editor. John Gruber and
> Aaron Swartz created Markdown in 2004 as a markup language that is appealing to human readers in its source code form.
> Markdown is widely used in blogging, instant messaging, online forums, collaborative software, documentation pages,
> and readme files.
>
> <cite>[Wikipedia](https://en.wikipedia.org/wiki/Markdown)</cite>

text2confl uses [flexmark](https://github.com/vsch/flexmark-java) library for parsing markdown files. In additional to
basic markdown features, some widely used extensions like tables and codeblocks are supported as well as some special
features that are useful for Confluence.

You can find information about supported features [on separate page](storage-formats/markdown.md).

## AsciiDoc

> AsciiDoc is a human-readable document format, semantically equivalent to DocBook XML, but using plain-text mark-up
> conventions. AsciiDoc documents can be created using any text editor and read “as-is”, or rendered to HTML or any other
> format supported by a DocBook tool-chain, i.e. PDF, TeX, Unix manpages, e-books, slide presentations, etc. Common
> file extensions for AsciiDoc files are txt (as encouraged by AsciiDoc's creator) and adoc.
> 
> <cite>[Wikipedia](https://en.wikipedia.org/wiki/AsciiDoc)</cite>

text2confl uses [asciidoctorj](https://github.com/asciidoctor/asciidoctorj) library for parsing asciidoc files. Many asciidoc features are supported and mapped to Confluence elements.

You can find information about supported features [on separate page](storage-formats/asciidoc.adoc).