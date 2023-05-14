---
labels: supported-format,markdown
---

# Markdown - code blocks

## Inline code

Inline code: `printf("Hello world!")`

## Code blocks

Code blocks are also supported including language tags:

```java
class Test {
    public static void main(String[] args) {
        System.out.println("Hello world!");
    }
}
```

With attributes, you can customize code block element. They can be put after language tag

```kotlin {title="hello.kt"}
println("Hello world!")
```

or after code block (on separate line)

```kotlin
println("Same here!")
``` 

{title="hello.kt"}

You can find details about supported languages and other code block attributes on separate
page: [Code highlight](../../user-guide/code-blocks.md)

