# Storage formats

Right now only markdown is supported.

## Markdown

> Markdown is a lightweight markup language for creating formatted text using a plain-text editor. John Gruber and Aaron Swartz created Markdown in 2004 as a markup language that is appealing to human readers in its source code form. Markdown is widely used in blogging, instant messaging, online forums, collaborative software, documentation pages, and readme files.
>
> <cite>[Wikipedia](https://en.wikipedia.org/wiki/Markdown)</cite>

text2confl uses [flexmark](https://github.com/vsch/flexmark-java) library for parsing markdown files. In additional to
basic markdown features, some widely used extensions like tables and codeblocks are supported as well as some special
features that are useful for Confluence.

You can find description of supported features [on separate page](storage-formats/markdown.md).