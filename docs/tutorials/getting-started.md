---
title: Getting started
---

# Getting started

In this tutorial, you will publish your first Markdown page to Confluence using text2confl. By the end, you will have a working setup you can build on.

**What you need before starting:**

- A Confluence instance (Cloud or Server/Data Center) where you have permission to create pages
- A Confluence API token or personal access token (see below)
- A terminal on macOS, Linux, or WSL

---

## Step 1 — Download text2confl

Go to the [releases page](https://github.com/zeldigas/text2confl/releases) and download the latest archive for your platform. Extract it:

```shell
tar xzf text2confl-*.tar.gz
```

The archive contains a `text2confl` executable. Move it somewhere on your `PATH`:

```shell
mv text2confl /usr/local/bin/text2confl
```

Verify it works:

```shell
text2confl --version
```

---

## Step 2 — Get a Confluence API token

**Confluence Cloud:** Go to [Atlassian account settings](https://id.atlassian.com/manage-profile/security/api-tokens) and create an API token. Your username is your email address, and the token is your password.

**Confluence Server/Data Center:** Create a personal access token in your Confluence profile settings under "Personal Access Tokens". Use the token with the `--access-token` flag (no username needed).

---

## Step 3 — Create your documentation directory

Create a directory for your docs:

```shell
mkdir my-docs
cd my-docs
```

---

## Step 4 — Create a configuration file

Create `.text2confl.yml` in your docs directory with the following content:

```yaml
server: https://yoursite.atlassian.net/wiki
space: MYSPACE
docs-location: https://github.com/yourorg/yourrepo/tree/main/my-docs/
```

Replace:
- `yoursite` with your Atlassian site name
- `MYSPACE` with the space key where you want to publish (found in the Confluence space URL)
- The `docs-location` URL with the location of your docs in version control (used to generate a source link on each page)

If you're using Confluence Server/Data Center, set `server` to your server URL, e.g. `https://confluence.example.org`.

---

## Step 5 — Create your first page

Create a Markdown file in the docs directory:

```shell
cat > hello-world.md << 'EOF'
# Hello from text2confl

This page was published automatically using [text2confl](https://github.com/zeldigas/text2confl).

## What is text2confl?

text2confl converts Markdown and AsciiDoc files into Confluence pages. You write docs as plain text files, commit them to version control, and text2confl keeps your Confluence space in sync.
EOF
```

---

## Step 6 — Upload to Confluence

**For Confluence Cloud** (email + API token):

```shell
text2confl upload --docs . \
  --user your@email.com \
  --password YOUR_API_TOKEN
```

**For Confluence Server/Data Center** (personal access token):

```shell
text2confl upload --docs . \
  --access-token YOUR_PERSONAL_ACCESS_TOKEN
```

text2confl will scan the directory, convert `hello-world.md` to Confluence Storage Format, and publish it as a new page in your space.

---

## Step 7 — See the result

Open your Confluence space. You should see a new page titled **"Hello from text2confl"** with the content you wrote.

At the top of the page, text2confl adds a note pointing to the source file in version control (using the `docs-location` you configured).

---

## What's next

You now have a working docs-as-code pipeline. Here are the natural next steps:

- **Add more pages** — create more `.md` files and subdirectories; subdirectory structure becomes the Confluence page hierarchy
- **Authenticate without command-line flags** — store credentials in an environment variable or config file: [Authenticate with Confluence](../how-to/authenticate.md)
- **Run in CI/CD** — use Docker to run text2confl in pipelines: [Run with Docker](../how-to/run-with-docker.md)
- **Learn what you can put in your pages** — browse the [Reference](../reference.md) for supported Markdown and AsciiDoc features
