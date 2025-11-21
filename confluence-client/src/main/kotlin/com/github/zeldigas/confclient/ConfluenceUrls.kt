package com.github.zeldigas.confclient

import io.ktor.http.*

fun makeLink(baseUrl: String, linkFromApi: String, rootApiLink: Boolean) =
    makeLink(Url(baseUrl), linkFromApi, rootApiLink)

fun makeLink(confluenceBaseUrl: Url, linkFromApi: String, rootApiLink: Boolean): Url {
    val segments = linkFromApi.split('/').filter { it.isNotEmpty() }

    if (rootApiLink) {
        return URLBuilder(confluenceBaseUrl).apply { path("") }.appendEncodedPathSegments(segments).build()
    } else {
        return URLBuilder(confluenceBaseUrl).appendEncodedPathSegments(segments).build()
    }
}