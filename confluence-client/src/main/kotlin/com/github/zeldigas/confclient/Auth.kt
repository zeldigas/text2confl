package com.github.zeldigas.confclient

import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*

interface ConfluenceAuth {

    fun create(auth: AuthConfig)

}

data class PasswordAuth(private val username: String, private val password: String) : ConfluenceAuth {
    override fun create(auth: AuthConfig) {
        val creds = BasicAuthCredentials(username, password)
        auth.basic {
            credentials { creds }
            sendWithoutRequest { true }
        }
    }

    override fun toString(): String {
        return "PasswordAuth(username='$username')"
    }

}

data class TokenAuth(private val token: String) : ConfluenceAuth {
    override fun create(auth: AuthConfig) {
        auth.bearer {
            loadTokens { BearerTokens(token, "") }
            sendWithoutRequest { true }
        }
    }
}