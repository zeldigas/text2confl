package com.github.zeldigas.text2confl.core.upload

import java.nio.file.Path

open class ContentUploadException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

open class ContentCleanupException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class VirtualPageNotFound(val source: Path, val title: String, val space: String) :
    ContentUploadException("${source} defined as virtual, but $space space does not have page with title: $title")