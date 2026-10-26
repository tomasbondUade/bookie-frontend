package org.bookie.app.data.remote

fun interface AccessTokenProvider {
    suspend fun getAccessToken(): String?
}
