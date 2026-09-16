package com.github.zeldigas.confclient.model

data class Space(
    val id: Long,
    val key: String,
    val name: String,
    val homepageId: String?,
    val homepage: ConfluencePage?
)

data class SpaceSearchResult(
    val results: List<Space>
)