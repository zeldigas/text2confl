# Virtual pages

If you don't plan to manage all your wiki pages as code, then there is a big chance for you to face with the following
challenge - you have pages that you want to store as code, but they can be in various places of your space tree.

Consider the following example:

* there is a number of pages under some parent
* parent page uses pretty complex formatting -
  multiples column pages with complex macros and other stuff so it does not make sense (or ever possible) to mange it as
  code
* child pages are simple or for any other reason fit well in idea of managing them as text files and sync with
  text2confl.

It can look like this, where `(managed)` marks pages that we want to manage with text2confl:

```text {title="Wiki pages tree"}
├── Parent page
│   ├── Page a (managed)
│   ├── Page b (managed)
├── Another page (managed)
```

In such situation, you might consider putting all pages in flat structure and specifying `parent` attribute (e.g. in
Markdown's front matter block), but this is really not that good, because you drift your files structure from you wiki
page structure.

For such scenarios, text2confl supports special page attribute that makes it treating page as "virtual": `_virtual_`.
Virtual pages are treated as regular pages in scanned file tree, but their content is kept unmanaged. Only location (
parent page) is tracked for such pages.

`_virtual_` attribute allows you to organize pages in the following way:

```text {title="Files structure"}
├── another-page.md
├── parent-page.md
├── parent-page
│   ├── page-a.md
│   ├── page-b.md
```

With `parent-page.md` content:

```markdown {title=parent-page.md}
---
title: Parent page
_virtual_: true
---
```
