package com.github.zeldigas.text2confl.convert.confluence

interface UserResolver {

    suspend fun resolveUser(email: String): UserReference?

    suspend fun resolveUsers(users: List<String>): Map<String, UserReference>

    fun registerReferencedUsers(email: List<String>)

    data class UserReference(val format: UserIdFormat, val value: String)
    enum class UserIdFormat {
        ACCOUNT_ID, USERNAME
    }

}