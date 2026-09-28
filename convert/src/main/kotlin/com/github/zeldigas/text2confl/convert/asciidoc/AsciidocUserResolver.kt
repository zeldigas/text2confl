package com.github.zeldigas.text2confl.convert.asciidoc

import com.github.zeldigas.text2confl.convert.confluence.UserResolver
import kotlinx.coroutines.runBlocking

class AsciidocUserResolver(val resolver: UserResolver) {

    fun resolve(name: String): Map<String, String>? {
        return runBlocking { resolver.resolveUser(name) }?.let { user ->
            when (user.format) {
                UserResolver.UserIdFormat.USERNAME -> mapOf("attr" to "ri:username", "value" to user.value)
                UserResolver.UserIdFormat.ACCOUNT_ID -> mapOf("attr" to "ri:account-id", "value" to user.value)
            }
        }

    }

}