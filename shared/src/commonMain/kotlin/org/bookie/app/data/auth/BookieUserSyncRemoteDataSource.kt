package org.bookie.app.data.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

interface BookieUserSyncRemoteDataSource {
    suspend fun sync(username: String)
}

/**
 * Receives a Bookie HTTP client configured with an AccessTokenProvider.
 * The backend identifies the user from Authorization, never from the request body.
 */
class KtorBookieUserSyncRemoteDataSource(
    private val client: HttpClient,
) : BookieUserSyncRemoteDataSource {
    override suspend fun sync(username: String) {
        client.post("users/sync") {
            contentType(ContentType.Application.Json)
            setBody(SyncUserDto(username))
        }
    }
}
