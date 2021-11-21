package com.github.zeldigas.confclient.model

data class Space(
    val id: Int,
    val key:String,
    val name:String,
    val homepage:ConfluencePage?
)