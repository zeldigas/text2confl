package com.github.zeldigas.confclient

interface ConfluenceUserSearchClient {

    suspend fun findUserIdByEmail(email: String): String?

}