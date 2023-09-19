package com.github.zeldigas.confclient

import com.github.zeldigas.confclient.model.PageProperty
import com.github.zeldigas.confclient.model.PropertyVersion
import java.nio.file.Path

data class PagePropertyInput(val value: Any, val version: PropertyVersion) {
    companion object {
        fun newProperty(value: Any) = PagePropertyInput(value, PropertyVersion(1))
        fun updateOf(property: PageProperty, newValue: Any) =
            PagePropertyInput(newValue, PropertyVersion(property.version.number + 1))
    }
}

data class PageContentInput(
    val parentPage: String?,
    val title: String,
    val content: String,
    val space: String? = null,
    val version: Int = 1
)

data class PageUpdateOptions(val notifyWatchers: Boolean = true, val message: String?)

data class PageAttachmentInput(
    val name: String,
    val content: Path,
    val comment: String?,
    val contentType: String? = null
)