package com.github.zeldigas.text2confl.core.users

import com.github.zeldigas.text2confl.convert.confluence.UserResolver

class ServerUserResolver : UserResolver {
    override suspend fun resolveUser(email: String): UserResolver.UserReference = user(email)

    private fun user(email: String): UserResolver.UserReference = UserResolver.UserReference(
        format = UserResolver.UserIdFormat.USERNAME, value = email
    )

    override suspend fun resolveUsers(users: List<String>): Map<String, UserResolver.UserReference> =
        users.associateBy({ it }, { user(it) })

    override fun registerReferencedUsers(email: List<String>) {
        // nothing to do here
    }
}