package com.github.zeldigas.text2confl.core.users

import com.github.zeldigas.confclient.model.User

interface UserResolver {

    suspend fun resolveUser(email: String): String?

    suspend fun resolveUsers(users: List<String>): Map<String, String>

}