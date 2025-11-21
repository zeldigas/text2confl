package com.github.zeldigas.confclient

import com.sletmoe.bucket4k.SuspendingBucket
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import java.time.Duration

private val logger = KotlinLogging.logger {}

private val RATE_LIMIT_HEADERS = setOf("X-RateLimit-Limit", "X-RateLimit-Remaining", "X-RateLimit-NearLimit")
private val NEAR_RATE_LIMIT_HEADERS =
    setOf("Beta-Retry-After", "X-Beta-RateLimit-NearLimit", "X-Beta-RateLimit-Reason", "X-Beta-RateLimit-Reset")

data class RateLimit(
    val rps: Int? = null,
    val honorTooManyRequests: Boolean = true,
    val retryCount: Int = 5,
)

internal fun <T : HttpClientEngineConfig> HttpClientConfig<T>.configureRateLimitRetries(config: ConfluenceClientConfig) {
    val statusToRetry = setOf(429, 503)
    install(HttpRequestRetry) {
        retryIf(config.rateLimit.retryCount) { _, response ->
            val needToRetry = response.status.value in statusToRetry
            if (RATE_LIMIT_HEADERS.any { it in response.headers }) {
                logger.warn {
                    "Rate limit information: ${collectHeaders(response)}"
                }
            }
            if (NEAR_RATE_LIMIT_HEADERS.any { it in response.headers }) {
                logger.info {
                    "Rate limit will be reached soon: ${collectHeaders(response)}"
                }
            }
            needToRetry
        }
        exponentialDelay()
    }
}

private fun collectHeaders(response: HttpResponse): Map<String, String?> {
    val data = buildMap {
        (NEAR_RATE_LIMIT_HEADERS + RATE_LIMIT_HEADERS).forEach {
            if (it in response.headers) {
                put(it, response.headers[it])
            }
        }
    }
    return data
}

internal fun HttpClient.limitRequestsRate(config: ConfluenceClientConfig) {
    if (config.rateLimit.rps != null) {
        val bucket = SuspendingBucket.build {
            val rps = config.rateLimit.rps.toLong()
            addLimit { capacity(rps).refillGreedy(rps, Duration.ofSeconds(1)) }
        }
        plugin(HttpSend).intercept { request ->
            bucket.consume(1)
            execute(request)
        }
    }
}