package com.github.zeldigas.confclient

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*

private val logger = KotlinLogging.logger { }

class PagedFetcher<R>(
    val baseUrl: Url,
    val rootApiLinks: Boolean,
    val loader: suspend (Url) -> R
) {

    data class Page<T>(val items: List<T>, val nextLink: String?)

    suspend fun <T> fetchAll(
        initial: R,
        extractor: (data: R) -> Page<T>
    ): List<T> {
        return buildList {
            var current = extractor(initial)
            addAll(current.items)
            while (current.nextLink != null) {
                val nextPage = makeLink(baseUrl, current.nextLink, rootApiLinks)
                logger.debug { "Loading next page: $nextPage" }
                current = extractor(loader(nextPage))
                if (current.items.isEmpty()) {
                    break
                } else {
                    addAll(current.items)
                }
            }
        }
    }

}