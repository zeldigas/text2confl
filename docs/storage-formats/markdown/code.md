---
labels: supported-format,markdown
---

# Markdown - code blocks

## Inline code

<table>
<thead>
<tr><th>Markdown</th><th>Confluence</th></tr>
</thead>
<tbody><tr>
<td>

```markdown
Inline code: `printf("Hello world!")`
```

</td><td>

Inline code: `printf("Hello world!")`

</td></tr></tbody></table>

## Code blocks

Code blocks are also supported including language tags:

<table>
<thead>
<tr><th>Markdown</th><th>Confluence</th></tr>
</thead>
<tbody><tr>
<td>

````markdown
```java
class Test {
    public static void main(String[] args) {
        System.out.println("Hello world!");
    }
}
```
````

</td><td>

```java
class Test {
    public static void main(String[] args) {
        System.out.println("Hello world!");
    }
}
```

</td></tr></tbody></table>

With attributes, you can customize code block element. They can be put after language tag

<table>
<thead>
<tr><th>Markdown</th><th>Confluence</th></tr>
</thead>
<tbody><tr>
<td>

````markdown
```kotlin {title="hello.kt"}
println("Hello world!")
```
````

</td><td>

```kotlin {title="hello.kt"}
println("Hello world!")
```

</td></tr></tbody></table>

or after code block (on separate line)

<table>
<thead>
<tr><th>Markdown</th><th>Confluence</th></tr>
</thead>
<tbody><tr>
<td>

````markdown
```kotlin
println("Same here!")
``` 

{title="hello.kt"}
````

</td><td>

```kotlin
println("Same here!")
``` 

{title="hello.kt"}

</td></tr></tbody></table>

You can find details about supported languages and other code block attributes on separate
page: [Code highlight](../../user-guide/code-blocks.md)

