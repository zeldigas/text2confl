package com.github.zeldigas.confclient

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.JsonConvertException
import io.ktor.util.toMap

internal suspend inline fun <reified T> HttpResponse.readApiResponse(expectSuccess: Boolean = false,
                                                                     errorParser: suspend HttpResponse.() -> Exception): T {
    val contentType = contentType()
    val jsonResponse = contentType != null && ContentType.Application.Json.match(contentType)
    if (expectSuccess && !status.isSuccess()) {
        if (jsonResponse) {
            throw errorParser()
        } else {
            throwUnknownError()
        }
    }
    if (jsonResponse || T::class == String::class) {
        try {
            return body<T>()
        } catch (_: JsonConvertException) {
            throw errorParser()
        }
    } else {
        throwUnknownError()
    }
}

private suspend fun HttpResponse.throwUnknownError(): Nothing {
    throw UnknownConfluenceErrorException(requestDetails(), status.value, headers.toMap(), bodyAsText())
}

internal fun HttpResponse.requestDetails(): RequestDetails = RequestDetails(
    method = this.call.request.method.value,
    url = this.call.request.url.toString()
)
