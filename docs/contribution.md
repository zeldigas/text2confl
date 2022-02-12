# Contributing to codebase

# Adding support for Confluence formatting features

Wiki pages are stored in confluence in special xml-like format that is called "storage format".

If you find some confluence formatting feature missing and would like to add them, here is the recommended steps to take.

1. Check [Confluence storage format](https://confluence.atlassian.com/doc/confluence-storage-format-790796544.html) documentation. It's a reference guide for basic functionality like text styling or embedding images on page that describe how all these basics should be represented in storage format 
2. Go to WYSIWYG editor and write there desired content after that save page and view it in storage format. Such an option is available in page options 
3. Now find the place in code to adjust (or add if this is something new) and transform AST node to proper storage format content
