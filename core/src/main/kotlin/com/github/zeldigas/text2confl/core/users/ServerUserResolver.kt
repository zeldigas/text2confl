package com.github.zeldigas.text2confl.core.users

class ServerUserResolver : UserResolver {
    override suspend fun resolveUser(email: String): String = email

    override suspend fun resolveUsers(users: List<String>): Map<String, String> = users.associateBy({ it }, { it })
}