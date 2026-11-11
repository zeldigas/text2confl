---
labels: contributing
---

# Design: preserving inline comments on page update

Status: **proposal**. Tracking issue: [#200](https://github.com/zeldigas/text2confl/issues/200).

Background research:

- [Foliant Confluence backend](https://github.com/foliant-docs/foliantcontrib.confluence/tree/66465d2aed3b4b4003f2ee6c684e51062878f4c5)
- [md2conf](https://github.com/hunyadi/md2conf/commit/b8528312583170ca2dcf014c6bfd774c441fb46d)

## 1. Problem

Readers leave inline comments on published pages. Confluence stores each one as a marker inside the page body:

```xml

<p>Retries use <ac:inline-comment-marker ac:ref="87342740-a564-...">exponential backoff</ac:inline-comment-marker>.
</p>
```

The comment itself (author, text, replies, status) lives elsewhere and points back to the marker through `ac:ref`. When
text2confl updates a page, it replaces the whole body with freshly generated content that has no markers, so every
comment on the page becomes detached. Depending on the Confluence version, the comment then shows up as resolved or
orphaned. Either way, the discussion is lost from the page.

The task is to carry markers from the current server body into the generated body before upload, placed on the same
text, while the rest of the generated content stays exactly as the converter produced it.

## 2. Goals and non-goals

Goals:

- Keep markers on text that did not change, including when paragraphs are moved, inserted or deleted around it.
- Keep markers when the paragraph was edited elsewhere, outside the commented text.
- Work the same for Markdown and AsciiDoc, on Cloud and on Server/Data Center.
- Add no extra requests for pages that are not updated, and never cause an update just because comments changed.
- Report, by name, every comment that could not be kept. Never drop one silently.
- Keep the current behavior available and make the feature easy to switch off.

Non-goals (for the first version):

- Moving a comment onto *rewritten* text. Small edits inside the commented text are handled (§8.4). If most of it was
  replaced, the comment is reported as lost. Neither tool does this well (Foliant's attempt is reviewed in §3), and a
  comment placed on the wrong text is worse than a comment that is reported as lost.
- Creating, resolving or editing comments through the comments API.
- Protecting against a comment added between our read of the page and our write. This is the same race every
  "read-modify-write" tool has (see §9).
- Supporting text edits made in Confluence. text2confl owns the page content, and manual edits are overwritten on the
  next upload, as they are today. If a comment sits on text that someone added or changed by hand, it is reported as
  lost.

**Assumption:** the text of the page on the server is the text text2confl uploaded last time. Confluence can change the
markup (§4) but not the text. Readers only add comments. So the text differences between the server body and the new
generated body come **only from changes to the source files**. That is why matching by flattened text (§8.2) is enough,
and why no merging of concurrent edits is needed.

## 3. What the two existing tools do

|                                              | Foliant                                                                                                                    | md2conf                                                                          |
|----------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| Default                                      | on                                                                                                                         | off (`--comments keep` to enable)                                                |
| Unit of comparison                           | individual text nodes, diffed as a sequence over the whole page                                                            | paragraphs/headings paired by similarity, then a character diff inside each pair |
| Unchanged commented text                     | restored exactly                                                                                                           | restored exactly                                                                 |
| Edit elsewhere in the paragraph              | the comment spreads over the whole text node                                                                               | restored exactly                                                                 |
| Commented text edited                        | the comment is stretched over the replacement text                                                                         | dropped                                                                          |
| Moved paragraph                              | the comment lands on neighbouring text                                                                                     | follows the paragraph                                                            |
| Comment across bold/italic etc.              | dropped if a single marker contains tags                                                                                   | supported (split into one marker per text run)                                   |
| Comments in lists without `<p>`, table cells | supported                                                                                                                  | **dropped silently**                                                             |
| Comments inside panels/links                 | probable crash                                                                                                             | supported in paragraphs                                                          |
| Interaction with change detection            | restores *before* hashing, so the page is fetched and diffed on every run, and comment activity alone triggers a re-upload | strips markers before comparison; restores only when an update is needed         |
| Reporting of lost comments                   | debug log                                                                                                                  | one warning per page                                                             |
| Tests                                        | none                                                                                                                       | 10 unit tests                                                                    |

Neither one is good enough to copy as is. md2conf's overall shape is the better base (block pairing, character-level
transfer, merge only when updating, opt-in). The design below keeps that shape and fixes the gaps both research notes
found: block coverage, duplicate paragraphs, whole-element markers, whitespace normalization, and reporting.

## 4. How Confluence rewrites the storage format

The body Confluence returns is not the body text2confl uploaded. Real samples are in
`core/src/test/resources/inline-comments/sample`,
made from `docs/how-to/configure-multi-tenancy.md`:

| File                                                     | Content                                                          |
|----------------------------------------------------------|------------------------------------------------------------------|
| `configure-multi-tenancy.html`                           | output of `text2confl convert`                                   |
| `configure-multi-tenancy.from-server.html`               | storage body right after upload (Cloud)                          |
| `configure-multi-tenancy.from-server-with-comments.html` | storage body after four inline comments were added in the editor |

**Right after upload** the changes are surface-level only. Structure and text stay the same:

- `ac:schema-version="1"` and a generated `ac:macro-id` are added to every `ac:structured-macro`;
- `ri:version-at-save` is added to `ri:page`, and attributes are reordered;
- newlines are added between blocks and after `<ac:rich-text-body>`;
- `style` values are normalized (`;font-size` without the space).

**After a comment is added** the editor saves the whole page again, and the structure changes, including parts nobody
touched:

| Generated                                                                         | Stored after commenting                                                                          |
|-----------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| `<p><ac:structured-macro ac:name="note">…</p>`                                    | the macro moves to the top level (no wrapping `<p>`), and `ac:parameter containerLevel` is added |
| `<span style="…; font-size: small;">text <a>link</a>.</span>`                     | split into `<span>text</span><a><span>link</span></a><span>.</span>`, and `font-size` is dropped |
| `<ac:link><ri:page ri:space-key="docs" …/><ac:plain-text-link-body><![CDATA[…]]>` | `ri:space-key` is dropped, and the body becomes `<ac:link-body>…</ac:link-body>` (rich text)     |
| code macro `language=shell`                                                       | `language=bash`, with a new `ac:macro-id`                                                        |
| `<ol><li>text</li>`                                                               | `<ol start="1"><li><p>text</p></li>`                                                             |

How the markers look:

- **Inside one text run:** `This guide shows ho<marker>w to configure multi-tenancy whe</marker>n …`. Boundaries fall in
  the middle of words.
- **Heading:** `<h2>Co<marker>nfigure a tenant</marker> ID<ac:structured-macro ac:name="anchor">…</h2>`.
- **Across three list items:** one marker with the same `ac:ref` in each `<li>`. In the first item the marker wraps
  text, a **whole `ac:link`** and more text. In the third item the marker stops at `<code>`, and a second marker with
  the same ref continues **inside** the `<code>` element (`<code><marker>rem</marker>ove-orphans…</code>`).
- **Over part of a link:** Confluence **splits the link in two** `ac:link` elements that point to the same page
  (`Multi-tenancy m` + `odel`) and wraps the second link, plus the text after it, in the marker. A marker is never put
  inside `ac:link-body`.

What this means for the design (together with the assumption in §2):

1. **The editor's rewrites are not our concern.** Ids, `macro-id`/`schema-version`/`version-at-save`, the
   `<li><p>` wrappers, `ac:link-body`, `ri:space-key`, `font-size`, the code language: Confluence adds or changes all
   of these itself, and does so again after every upload. The merge doesn't reproduce them, keep them or undo them. It
   only changes the places where comment markers go.
2. **The generated body is always the base.** The server body is read only to find out *where* the comments are.
   Nothing else is copied from it. md2conf's fast path ("blocks equal → copy the Confluence block") would bring editor
   changes into the result (`bash` instead of `shell`, the dropped `font-size`) and must not be used.
3. **Matching must not depend on attributes, ids, whitespace between blocks, or wrapper elements.** It works on
   flattened text per block (§8.2). `<li><p>…</p></li>` and `<li>…</li>` count as the same block, and a macro is
   compared by its name only (Confluence can rewrite parameters too, e.g. `shell` → `bash`).
4. **Links are text containers, not single units.** Their body text (`ac:link-body` or `ac:plain-text-link-body`) is
   part of the block text, and inserting markers must be able to split a link the way Confluence does (§8.5).
5. **A plain string comparison of the two bodies never matches**, even with no comments at all (see §7.2).

## 5. Proposal overview

```
convert (unchanged)          core: PageUploadOperationsImpl.updateExistingPage
──────────────────           ───────────────────────────────────────────────
page.content.body ──┐        change detector says "update"?
page.content.hash ──┤          │ no  → nothing to do (comments untouched)
                    │          │ yes → inline-comments = drop?  → upload generated body (today's behavior)
                    │          │       inline-comments = keep?
                    │          │         fetch server body (if not already loaded)
                    └──────────┼──────▶  InlineCommentsMerger.merge(serverBody, generatedBody)
                               │           → mergedBody + report (kept / lost refs)
                               │         log report, upload mergedBody
                               ▼
                  contenthash property = hash of *generated* content (unchanged rule)
```

Main decisions:

1. **Merge only on update.** The merge runs inside `updatePageContent`, after the change detector has decided the
   page needs an update. Unchanged pages are never fetched or touched, so their comments are safe.
2. **The hash stays the hash of the generated content.** The `contenthash` property keeps describing the converter
   output, not the merged body. A later run with unchanged sources is then a no-op, and adding or resolving a comment
   in Confluence never triggers a re-upload. Foliant gets this wrong.
3. **The merger is a pure function on two storage-format strings.** It has no Confluence client and no knowledge of
   Markdown or AsciiDoc, so it can be tested with fixtures and can't affect pages when the feature is off.
4. **Fast path.** If the server body has no `ac:inline-comment-marker`, the generated body is uploaded byte for byte,
   without being parsed and serialized again. This keeps the risk of the feature at zero for pages nobody commented on.

## 6. Configuration

A new option in `.text2confl.yml` and on the `upload` command, following the naming of `modification-check`:

```yaml
inline-comments: keep    # drop | keep
```

```
--inline-comments [drop|keep]
```

| Value  | Behavior                                                                                         |
|--------|--------------------------------------------------------------------------------------------------|
| `drop` | Today's behavior. The generated body is uploaded as is.                                          |
| `keep` | Markers are merged into the generated body. Comments that can't be kept are listed in a warning. |

Default: **`drop` for the first release**, then switch to `keep` once it has been used on real spaces. A default that
changes page bodies should not appear in a patch release, and md2conf made the same call.

A possible later value is `keep-or-skip`: if any open comment would be lost, skip updating that page and report it.
That needs the comment status, which means calling the comments API. It is left out of the first version (see §10).

The `DirectoryConfig` field would be `inlineComments: InlineCommentsMode = InlineCommentsMode.DROP`, and it would be
passed through `UploadConfig` into `PageUploadOperationsImpl` next to `pageContentChangeDetector`.

## 7. Upload flow changes

### 7.1 Fetching the server body

`findPageOnServer` loads the page without its body when the `HASH` detector is used. Two options:

- **A. Load the body for every page** by adding `SimplePageLoadOptions.Content` to the load options when the mode is
  `keep`. This is simple, but it transfers every page body on every run, even for pages that won't change.
- **B. Load the body only for pages that will be updated**, with an extra `getPageById(id, setOf(Content))` inside
  `updatePageContent`. This costs one request per *changed* page.

Recommendation: **B**. In a typical run only a few pages change, so B is far cheaper. If the `CONTENT` detector is
active, the body is already loaded and is reused.

The version from this fetch should be compared with `confluencePage.version`. If they differ, the page was edited
between the two reads. That case is rare, and it is enough to use the fresher body and version.

### 7.2 `CONTENT` change detector

`ChangeDetector.CONTENT` compares `serverPage.body.storage.value` with the generated body as plain strings. The samples
in §4 show that these strings never match, even right after an upload with no comments, because Confluence adds
`ac:macro-id`, `ri:version-at-save` and newlines. Once someone comments, the editor also changes the structure
(`<li><p>`, `ac:link-body`, the macro moved out of its `<p>`). Removing markers before the comparison is therefore not
enough: `CONTENT` would need a structural comparison that understands the editor's rewrites, which is a separate and
larger problem.

This feature does not try to fix `CONTENT`. With `keep` it still works (the page is uploaded every time, and the merge
keeps the comments), but the extra uploads make the race in §9 more likely. The user docs for `keep` should
recommend the `hash` detector, which is the default. Fixing `CONTENT` is worth a separate issue.

### 7.3 Dry run

`--dry` should run the merge too, using the real server body, and print the same report. That way users can see which
comments would be lost before switching `keep` on or publishing a large rewrite. Dry run only fakes write operations,
so a real read is fine.

### 7.4 Reporting

The merger returns, for every `ac:ref` found in the server body, whether it was kept, and if not, the commented text
(shortened) and the reason (`block-not-found`, `text-changed`, `unsupported-location`). Comments kept on edited text
(`adjusted`, §8.4) are listed too, with the old and new text, so the user can check them. `PageUploadOperationsImpl`
logs:

```
WARN Page "Retry policy": 2 of 5 inline comments could not be kept:
  - "exponential backoff" (ref 87342740…): commented text changed
  - "see table below" (ref 1c0a…): paragraph not found in new content
INFO Page "Retry policy": 1 inline comment moved to edited text:
  - "my old long text" -> "my long text" (ref 5b1e…)
```

Also add the counts to `PageOperationResult.ContentModified`, so they can appear in the final upload summary.

## 8. Merge algorithm

All steps work on parsed trees of the two bodies. Code lives in a new package
`com.github.zeldigas.text2confl.core.upload.comments`, with `InlineCommentsMerger` as the entry point.

### 8.1 Parsing and serialization

Use **jsoup** in XML mode (`Parser.xmlParser()`). jsoup is already on the classpath through the auto-fix feature; it
should become an explicit `core` dependency. `ac:`/`ri:` names are kept as plain tag names, and CDATA in code macros is
preserved. Serialization uses `prettyPrint(false)` and XHTML syntax.

Risk: re-serializing the generated body can change entities (`&nbsp;` → U+00A0), attribute quoting or empty-element
form. Confluence accepts all of these, but they must not change what the page looks like. Required safeguards:

- The fast path (§5) skips parsing whenever there are no markers.
- A round-trip test over the converter's existing golden outputs for Markdown and AsciiDoc: parse + serialize must give
  XML that is equivalent (same tree, same text after entity decoding).

### 8.2 Text model

For each body, build a list of **text blocks**. A text block is the nearest ancestor of text that is one of: `p`,
`h1`–`h6`, `li`, `td`, `th`, `dt`, `dd`, `blockquote`, `caption`, `figcaption`. Unlike md2conf, this covers tight
lists and table cells.

The editor adds wrappers that the generator doesn't emit (§4), so blocks are normalized before they are compared:

- A container block (`li`, `td`, `th`, `dd`, `blockquote`) whose only non-whitespace child is a single `<p>` is
  treated as **one block** with that `<p>`'s content. `<li><p>text</p></li>` from the editor and `<li>text</li>`
  from the generator then match. If a container has several paragraphs or mixed content, each `<p>` is its own block
  and the container keeps only the text outside them.
- A `<p>` that contains nothing but a block macro (the generated `<p><ac:structured-macro ac:name="note">`) is not
  a block. The macro's `ac:rich-text-body` paragraphs are.
- Attributes are never part of the comparison (`ac:macro-id`, `ac:schema-version`, `ri:version-at-save`,
  `ac:local-id`, `style`, `start`).

Each block has a **flattened text** built by walking its subtree in document order:

- Text nodes contribute their text. Whitespace is normalized for matching: runs of whitespace and U+00A0 become one
  space. An offset map back to the raw text is kept.
- Inline formatting (`strong`, `em`, `code`, `span`, `a`, `u`, `s`, `sub`, `sup`) is transparent: only its text
  counts. This is necessary because the editor splits and merges spans (§4).
- **`ac:link` is a text container.** Its text is the content of `ac:link-body` or `ac:plain-text-link-body` (the
  generator writes CDATA, the editor rewrites it to `ac:link-body`, and both give the same text). `ri:*` children
  give no text. If a link has no body, it counts as a single placeholder character, as below.
- Atomic inline elements (inline `ac:structured-macro` such as `status` or `anchor`, `ac:image`, `ac:emoticon`,
  `ac:inline-task-*`, `time`) contribute **one placeholder character** (U+FFFC) plus a signature of the element or
  macro name only. Parameters and `ac:macro-id` are not part of it, because Confluence can rewrite them. Their subtree
  is skipped, so a marker can't end up inside a macro parameter.
  The `anchor` macro in each generated heading becomes one trailing placeholder on both sides.
- `ac:rich-text-body` is not atomic. Its paragraphs are normal blocks of their own, so comments inside info/note
  panels and expand macros are supported.
- `ac:plain-text-body`, `ac:parameter` and CDATA outside link bodies are ignored. Confluence doesn't allow comments
  there.

From the server body, collect **comment spans**: for each `ac:ref`, the ranges `[start, end)` it covers in its block's
flattened text. Confluence splits a selection across formatting into several markers with the same ref, so adjacent
fragments in one block are joined into one range. A comment across several blocks gives one range per block, and each
range is transferred on its own. The comment counts as kept if any of its ranges is kept, and partially kept ranges are
reported.

Nested markers (one marker inside another) are flattened: each keeps its own range. Unlike Foliant, nothing is
dropped on the assumption that it is resolved.

The server blocks are then turned into "clean" text by removing the markers. Generated blocks are never touched up to
this point.

### 8.3 Pairing blocks

Only server blocks that contain a comment span need a partner. Pairing runs in three passes, each taking only blocks
that are still free (pairing is **one-to-one**):

1. **Exact.** Same block kind and equal normalized text. If several candidates qualify, the one whose relative
   position in the document (index ÷ block count) is closest wins. This handles duplicate paragraphs, which md2conf
   gets wrong.
2. **Similar.** Same block kind group (headings with headings, `p`/`li`/`dd`/`blockquote` together, `td`/`th`
   together). Score = token similarity `2·M / (|a| + |b|)` from a token-level diff of lowercased words and punctuation.
   All candidate pairs above the threshold are sorted by score (position distance breaks ties) and assigned greedily.
   The threshold depends on length: 0.8 for blocks of 10+ tokens, scaled down to 0.5 for very short ones. Then
   changing one word in a three-word heading doesn't lose an unchanged comment on another word.
3. **Anchor search** (fallback for blocks that are still unpaired). For each comment span, look for its exact commented
   text, with a few words of context on each side, anywhere in the remaining generated blocks. If it occurs exactly
   once, use it. This covers a paragraph that was split in two or merged with another, where block similarity drops
   but the commented sentence survived.

### 8.4 Transferring spans inside a pair

1. If the clean server block and the generated block have equal flattened text, reuse the ranges unchanged. Only the
   *ranges* are reused. Server content is never copied, because the editor may have rewritten the block's markup (§4).
2. Otherwise, run a character-level diff (Myers, via **java-diff-utils**) between the two flattened texts, then apply
   the graded rule below to each span.

**Mapping a span.** `start` maps to the new offset of the first original character of the span that survived (an
equal region), and `end` maps to the position right after the last one. Anything inserted between them becomes part
of the new range. If no character of the span survived, the span is lost.

**Graded rule.** Let `kept` be the share of the span's original characters that are still in equal regions:

| Case           | Condition                                                                                                                            | Result                                                    | Example                                                                        |
|----------------|--------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------|--------------------------------------------------------------------------------|
| Unchanged      | `kept = 1` and nothing inserted inside                                                                                               | kept, same text                                           | `my text` → `my text`                                                          |
| Only additions | `kept = 1`, text inserted inside                                                                                                     | kept, widened to the new text                             | `my text` → `my adjusted text`                                                 |
| Small edit     | the first and last characters of the span survive, at least ⅔ of the characters of the first and last word survive, and `kept ≥ 0.5` | kept on the mapped range, marked `adjusted` in the report | `my old long text` → `my long text`, `recieves requests` → `receives requests` |
| Rewritten      | otherwise                                                                                                                            | lost, reported as `text-changed`                          | `my text` → `your words`                                                       |

Notes:

- The ends are checked at **word** level. The edge characters must survive, because they fix the new position of the
  range. The rest of a boundary word may change a little: a typo fix (`recieves` → `receives`, `colour` → `color`)
  keeps the comment, a replaced word (`my` → `your`) loses it. For a span whose boundary falls inside a word (Confluence
  selections often do, see §4), only the part of that word inside the span is checked. The new range keeps
  the same offset inside the word.
- The 0.5 threshold is a starting value to tune on fixtures. It applies only after the block has already been paired
  (§8.3), so the surrounding paragraph is known to be the same one.
- This differs from md2conf, which drops the comment unless the commented text is unchanged. It is safe here because
  of the assumption in §2: text changes only through the user's own source edits, which are usually small.

### 8.5 Re-inserting markers

For each kept range in a generated block, insert markers in the same shape Confluence writes them (§4):

- **Wrap the largest runs of whole sibling nodes.** Walk the block's children. Contiguous siblings that lie entirely
  inside the range (text, whole `ac:link` elements, atomic elements) go into one marker together. This matches the
  first list item in the sample: one marker around text + `ac:link` + text.
- **Split text nodes at range boundaries.** If the boundary falls inside a text node, split it and wrap only the
  covered part.
- **Never wrap formatting, always descend into it.** For `strong`, `em`, `code`, `span`, `a` and so on, put the
  marker inside the element, even when the element is fully covered, and keep the same `ac:ref` on every fragment:
  `…(</marker><code><marker>rem</marker>ove…`, `<a href="…"><marker>whole link</marker></a>`. This is what Confluence
  writes (sample `links.commented.html`): formatting always stays outside the marker.
- **Split links that are only partly covered.** Markers are never placed inside `ac:link`. Instead, clone the
  `ac:link`, keeping the generated `ri:*` resource and attributes, divide the body text between the two clones at
  the boundary, and wrap the covered clone. This is exactly what Confluence did with `Multi-tenancy m` + `odel`. The
  generated body form is kept (CDATA stays CDATA).
- Wrapping never changes the flattened text, so ranges in the same block can be applied one after another in any
  order. They are applied from the start of the block (the longer range first when two start together). This nests
  overlapping comments the way Confluence does: in the overlap the later comment goes inside the earlier one's marker,
  and continues in its own marker after it (`<c1> b<c2>efore</c2></c1><c2> you st<c3>art</c3></c2><c3>.</c3>`).

Ranges are mapped back from normalized to raw offsets before splitting, so whitespace in the generated body is never
changed.

### 8.6 Complexity

Pairing is O (C·N) token diffs, where C is the number of commented server blocks and N is the number of generated
blocks. A precheck on token-count difference skips most pairs. C is small in practice, so pages with thousands of
blocks are not a problem. The character diff runs only once per pair.

## 9. Edge cases

| Case                                                                          | Behavior                                                                                                        |
|-------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| Server body has no markers                                                    | fast path, the generated body is uploaded unchanged                                                             |
| Page is new                                                                   | nothing to merge                                                                                                |
| Page unchanged (hash equal)                                                   | not updated, comments untouched                                                                                 |
| Paragraph moved, or paragraphs added/removed around it                        | pairing is independent of order, so comments follow the text                                                    |
| Duplicate paragraphs                                                          | one-to-one pairing using position, so each copy keeps its own comments                                          |
| Words added inside the commented text                                         | kept, range widened (§8.4)                                                                                      |
| Commented text partly edited, ends and ≥ 50% survive                          | kept on the mapped range, reported as `adjusted`                                                                |
| Commented text rewritten                                                      | comment reported as lost (`text-changed`)                                                                       |
| Paragraph deleted                                                             | reported as lost (`block-not-found`)                                                                            |
| Comment on a whole link or inline macro                                       | the element is wrapped as a whole if unchanged                                                                  |
| Comment on part of a link                                                     | the generated link is split into two links to the same target, and the covered part is wrapped (§8.5)           |
| Comment across list items                                                     | one range per item, each transferred on its own; partial survival is reported                                   |
| Editor rewrote markup (`<li><p>`, `ac:link-body`, split spans, new macro ids) | ignored by the text model (§8.2); the generated markup is uploaded                                              |
| Comment inside a panel/expand body                                            | supported as a normal block                                                                                     |
| Comment in a code block                                                       | can't exist in Confluence; any found are reported as `unsupported-location`                                     |
| Marker with no `ac:ref`                                                       | ignored                                                                                                         |
| Text edited by hand in Confluence                                             | not supported (§2); the manual edit is overwritten as today, and comments on that text are reported as lost     |
| Comment added between our read and our write                                  | lost, same race as every update; the version check in §7.1 narrows the window but can't close it                |
| Resolved comments                                                             | carried like open ones. The marker of a resolved comment is harmless, and filtering would need the comments API |
| Generated body fails to parse (should not happen after validation)            | log a warning and upload the generated body unmerged, never fail the upload because of comments                 |

## 10. Future extensions (not in the first version)

- **Comment status from the API.** Cloud: `GET /api/v2/pages/{id}/inline-comments` (returns `resolutionStatus`,
  `inlineMarkerRef`). Server/DC:
  `GET /rest/api/content/{id}/child/comment?expand=extensions.inlineProperties,extensions.resolution`
  (paginated). This makes possible:
    - `keep-or-skip` mode (don't update a page if an open comment would be lost);
    - showing the comment author and text in the loss report;
    - leaving out markers of resolved comments.
- **A setting for the §8.4 threshold**, or a strict mode that keeps only unchanged text, if users ask for one.
- **Applying the merge to the `convert` command** with a downloaded body, to preview the result locally.

## 11. Implementation plan

1. Prepare tests in `core/src/test/resources/inline-comments/sample`. They are the first
   fixture: generated body + `from-server-with-comments` body → the expected merged body in §11.1.
2. Add `InlineCommentsMerger` with the text model, pairing, transfer, insertion and report, plus unit tests using
   fixtures. Add `java-diff-utils` and an explicit `jsoup` dependency to `core`.
3. Add the round-trip safety test over the converter's golden outputs (§8.1).
4. Wire it in: config option, CLI flag, the lazy fetch in `updatePageContent`, logging, the result counts and dry-run
   support.
5. Document it in the user docs: reference (configuration option), explanation ("How inline comments are kept on
   update", with the rules from §8.4 and §9), and a how-to.
6. Manual check on a Cloud and a Data Center instance, saving real bodies (with `ac:local-id` attributes, `<p>` inside
   `<li>`, and `&nbsp;`) as extra fixtures.

### 11.1 Worked example: the sample page

Input: `configure-multi-tenancy.html` (generated) and `configure-multi-tenancy.from-server-with-comments.html` (server).
The expected result is the generated body unchanged, except for these blocks (`M(x)` is
`<ac:inline-comment-marker ac:ref="x">`, and `ri:page` attributes are shortened):

```xml

<p>This guide shows ho<M(7a6153ad)>w to configure multi-tenancy whe
</M>n multiple teams share …</p>

<h2>Co<M(9e95f5cb)>nfigure a tenant
</M> ID<ac:structured-macro ac:name="anchor">…</ac:structured-macro></h2>

<li>Pages witho<M(aae132d9)>ut a tenant are &quot;vacant&quot; and can be claimed by any tenant - except
<ac:link>
    <ri:page ri:content-title="[text2confl] Virtual pages" ri:space-key="docs"/>
    <ac:plain-text-link-body><![CDATA[virtual pages]]></ac:plain-text-link-body>
</ac:link>
, which are never modified.
</M></li>
<li><M(aae132d9)>A page belonging to another tenant cannot be updated; text2confl will report an error.
</M></li>
<li><M(aae132d9)>Managed-page cleanup (
</M><code><M(aae132d9)>rem
</M>ove-orphans: managed</code>) skips pages …</li>

<li>
<ac:link>
    <ri:page
    …Multi-tenancy model… />
    <ac:plain-text-link-body><![CDATA[Multi-tenancy m]]></ac:plain-text-link-body>
</ac:link>
<M(d05551ef)>
<ac:link>
    <ri:page
    …Multi-tenancy model… />
    <ac:plain-text-link-body><![CDATA[odel]]></ac:plain-text-link-body>
</ac:link>
-
</M>conceptual explanation …</li>
```

Everything else stays as generated: the note macro in its `<p>` with `font-size: small`, `language=shell`, no
`ac:macro-id`, `<li>` without `<p>`, and `ri:space-key` on links. The list items match even though the server has
`<li><p>` and `ac:link-body`, because their flattened texts are equal (§8.2).

### 11.2 Test fixtures (minimum set)

Taken from both research notes: unchanged page; edit before/after the comment in the same paragraph; edit inside the
comment, for each row of the §8.4 table (words added, a word removed, one end removed, rewritten); a span starting or
ending in the middle of a word; comment across bold/italic; two comments in one paragraph; overlapping comments;
paragraph moved;
paragraphs inserted before, between and after; duplicate paragraphs; paragraph split into two; short heading with one
word changed; comment in a tight list item, table cell, and info panel; comment on a link and on a status macro;
`&nbsp;` in the server body vs a plain space in the generated body; marker with no ref; empty generated body.

## 12. Open questions

1. **Default value.** Is `drop` → `keep` over two releases right, or should `keep` be the default from the start?
2. **What exactly happens to a comment whose marker disappears**, on current Cloud and on DC 8.x/9.x: resolved,
   orphaned or deleted? This affects how strongly the loss warning is worded, and whether `keep-or-skip` is worth adding
   early.
3. **Does adding an inline comment create a new page version** on Cloud and on DC? If it does, the version check in
   §7.1 also detects comments added during the run.
4. **Block kinds that AsciiDoc output adds** (for example the `div`-based structures some templates produce) may need
   to join the text-block list in §8.2. This will be checked against the golden outputs.
5. ~~**Markers inside an HTML `<a>`.**~~ Resolved by `links.commented.html` (Cloud): the marker always goes inside
   `<a>` and `<strong>`, even when the whole link is commented, and a comment crossing link boundaries becomes several
   markers with the same ref (§8.5). Comments in ADF panels (`ac:adf-extension`) are kept in `ac:adf-content` only;
   `ac:adf-fallback` gets a plain `<span class="inline-comment-marker">` and is ignored by the merge.
6. **Data Center.** All samples come from Cloud. A DC sample after commenting is needed to check whether its editor
   makes the same rewrites (`<li><p>`, `ac:link-body`) and splits links the same way.
7. **Does the comment action save the whole page again** (new version, editor conversion), as the samples suggest, or
   only add the marker? This ties into question 3.
