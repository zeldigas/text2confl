package com.github.zeldigas.text2confl.convert

import io.ktor.http.URLDecodeException
import io.ktor.http.decodeURLPart

/**
 * Decodes optionally url-encoded file path to regular name. `my%20spaced.txt` -> `my spaced.txt`
 *
 * If path contains `%` that is not a valid escape sequence (e.g. `50% off.png`), path is returned as is.
 */
internal fun String.decodeFilePath(): String {
    if ("%" !in this) return this
    return try {
        this.decodeURLPart()
    } catch (_: URLDecodeException) {
        this
    }
}

internal fun String.urlLike(): Boolean = URI_DETECTOR.find(this) != null

private val URI_DETECTOR = "^[a-zA-Z][a-zA-Z0-9.+-]+:/{0,2}".toRegex(RegexOption.IGNORE_CASE)