package com.github.zeldigas.confclient.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.ZonedDateTime

data class ConfluencePage(
    val id: String,
    val type: ContentType,
    val status: String,
    val title: String,
    val metadata: PageMetadata?,
    val body: PageBody?,
    val version: PageVersionInfo?,
    val children: PageChildren?,
    val ancestors: List<ConfluencePage>?,
    val space: Space? = null,
    @JsonProperty("_links")
    val links: Map<String, String> = emptyMap()
) {
    fun pageProperty(name: String): PageProperty? {
        return metadata?.properties?.get(name)
    }
}

enum class ContentType {
    page, blogpost
}

data class PageMetadata(
    val labels: PageLabels?,
    @JsonIgnoreProperties("_links", "_expandable")
    val properties: Map<String, PageProperty> = emptyMap()
)

data class PageLabels(
    val results: List<Label>,
    val size: Int
)

data class Label(
    val prefix: String,
    val name: String,
    val id: String,
    val label: String? = null
)

data class PageProperty(
    val id: String,
    val key: String,
    val value: Any?,
    val version: PropertyVersion
)

data class PropertyVersion(
    val number: Int
)

data class PageBody(
    val storage: StorageFormat?
)

data class StorageFormat(
    val value: String,
    val representation: String
)

data class PageVersionInfo(
    val number: Int,
    val minorEdit: Boolean,
    @JsonProperty("when") val createdAt: ZonedDateTime?
)

data class PageChildren(
    val attachment: PageAttachments?
)

data class PageAttachments(
    val start: Int? = null,
    val limit: Int? = null,
    val size: Int? = null,
    val results: List<Attachment> = emptyList(),
    @JsonProperty("_links")
    val links: Map<String, String> = emptyMap()
)

data class Attachment(
    val id: String,
    val title: String,
    val metadata: Map<String, Any?> = emptyMap(),
    @JsonProperty("_links")
    val links: Map<String, String> = emptyMap()
)

