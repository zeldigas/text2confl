package com.github.zeldigas.confclient

import com.github.zeldigas.confclient.model.PageProperty
import com.github.zeldigas.confclient.model.PropertyVersion

data class PagePropertyInput(val value: String, val version: PropertyVersion) {
    companion object {
        fun newProperty(value: String) = PagePropertyInput(value, PropertyVersion(1))
        fun updateOf(property: PageProperty, newValue: String) =
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

data class UpdateParameters(val notifyWatchers: Boolean = true, val message: String?)