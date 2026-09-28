package com.github.zeldigas.text2confl.convert.confluence

object TestUserResolver : UserResolver {

    override suspend fun resolveUser(email: String): UserResolver.UserReference? {
        return UserResolver.UserReference(UserResolver.UserIdFormat.USERNAME, email)
    }

    override suspend fun resolveUsers(users: List<String>): Map<String, UserResolver.UserReference> {
        return users.map { it to UserResolver.UserReference(UserResolver.UserIdFormat.USERNAME, it) }.toMap()
    }

    override fun registerReferencedUsers(email: List<String>) {
    }
}