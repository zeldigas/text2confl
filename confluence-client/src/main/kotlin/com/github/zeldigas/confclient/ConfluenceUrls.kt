package com.github.zeldigas.confclient

import io.ktor.http.*

fun makeLink(baseUrl: String, linkFromApi: String) = makeLink(Url(baseUrl), linkFromApi)

fun makeLink(confluenceBaseUrl: Url, linkFromApi: String):Url {
    val segments = linkFromApi.split('/').filter { it.isNotEmpty() }

    return URLBuilder(confluenceBaseUrl).appendEncodedPathSegments(segments).build()
}