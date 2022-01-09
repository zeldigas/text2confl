package com.github.zeldigas.confclient

import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*

interface ConfluenceAuth {

    fun create(auth:Auth)

}

data class PasswordAuth(private val username: String, private val password:String) : ConfluenceAuth {
    override fun create(auth:Auth) {
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

data class TokenAuth(private val token:String) : ConfluenceAuth {
    override fun create(auth: Auth) {
        auth.bearer {
            loadTokens { BearerTokens(token, "") }
            sendWithoutRequest { true }
        }
    }
}