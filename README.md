
# text2confl

Is a tool for publishing documentation written in structured text formats like markdown to Confluence (either server or cloud edition).

## Scope

`text2confl` does not care and assume the purpose of publishing process and the type of content you are going to upload - it can be some formal docs managed as a code or todo list or some report of tickets from jira. Due to this fact there is no clear end state of features (so far) that should be supported and features that should not be in this tool. If you find something missing - feel free to create an issue and describe your request.

## Design and usability goals

Here are a number of items that tool tries to follow:
1. Provide good features coverage for every supported source format - if you pick asciidoc you should feel comfortable by using its features and not always bumping into the fact that something is not supported.

   The important note here is that feature should somehow be mapped to Confluence feature of formatting
2. Provide reasonable defaults and auto-detection of parameters where possible - if you use Confluence Cloud there is no need to specify editor version unless you have some non-standard needs
3. Support both Confluence Server and Cloud and be aware of their differences in editors or features
4. If source format supports extensibility and customization - support it. There should be no need to create a fork just because you need to add some macros for your asciidoc documents.

## Supported source formats

By design multiple formats can be supported and even mixed together thus given you the option to mix and match one type of documents like markdown with another type of documents like asciidoc and be able to select what to use for every page.

Here are the state of support of formats:

1. 🚧 Markdown *in progress*
2. 📅️ Asciidoc *planned️*

