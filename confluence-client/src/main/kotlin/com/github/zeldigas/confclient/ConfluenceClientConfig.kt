package com.github.zeldigas.confclient

import io.ktor.client.plugins.logging.*
import io.ktor.http.*

/**
 * Parameters for whole upload procedure
 */
data class ConfluenceClientConfig(
    val server: Url,
    val skipSsl: Boolean,
    val auth: ConfluenceAuth,
    val httpLogLevel: LogLevel = LogLevel.NONE,
    val requestTimeout: Long = 30000,
    val connectTimeout: Long = 30000,
    val socketTimeout: Long = 30000
)