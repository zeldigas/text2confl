package com.github.zeldigas.confclient

import io.ktor.http.*
import java.net.URL

/**
 * Parameters for whole upload procedure
 */
data class ConfluenceClientConfig(
    val server: Url,
    val skipSsl: Boolean,
    val auth: ConfluenceAuth
)