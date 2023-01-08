# Page attributes

Every document can have _attributes_ associated with it. This page describes attributes that are special for
**text2confl** and treated by it in specific way.

[TOC]

## Document title

Document title (name) can be defined in the following ways (from top priority to bottom):

1. `title` attribute
2. first level heading at the top of document. If it is used, heading will be removed from resulting document to avoid
   duplication (if not done by underlying format automatically)
3. name of file

Recommended approach is to use first level heading as it provides also good-looking content outside of
Confluence. Usage of `title` attribute also fine, especially if you prefer to hide as much confluence-specific things
from content as possible.

### Example 1 - title will be taken from *yaml front matter*

```markdown {title=my-page.md}
---
title: My page
---
Document content
```

Result title - `My page`

### Example 2 - title will be taken from *yaml front matter* regardless presence of top level header

```markdown {title=my-page.md}
---
title: My page
---

# Header that is not used as a title

Document content
```

Result title - `My page`

### Example 3 - title will be taken from first level header

```markdown {title=my-page.md}
# My Page

Document content
```

Result title - `My page`

**Note**: resulting page content will not include first level header

### Example 4 - title will be taken from filename

```markdown {title=my-page.md}
Document content
```

Resulting title - `my-page`

## Page labels

Confluence page labels can be set by `labels` attribute. Attribute can be specified as comma-separated string or a list
of strings (if file format supports this).

Example - markdown document that will have 3 labels (`one`, `two`, `three`):

```markdown
---
labels: one, two, three
---
Document content
```

## Custom parent

It is possible to specify custom parent for every page using `parent` or `parentId`. While it's not restricted on every
level, it makes sense to use this only for top level pages for configuring
location of subtree (root page and all its children).

Attribute `parentId` should contain id of parent page and attribute `parent` should contain parent page title. When
specified both, `parentId` is used and `parent` will be ignored.

Example - markdown document with custom parent `Custom Parent Page`. Such document will be uploaded under this page if
it exists. When custom parent does not exist, upload procedure fails.

```markdown
---
parent: Custom Parent Page
---
Document content
```

## Virtual pages

Special attribute `_virtual_` allows you to define _virtual pages_ in tree structure. Read more
on [dedicated page](./virtual-pages.md).