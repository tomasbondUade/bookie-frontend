package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.appendPathSegments
import org.bookie.app.data.remote.dto.ReadingCompatibilityDto

interface ReadingCompatibilityRemoteDataSource {
    suspend fun getCompatibility(targetUserId: String): ReadingCompatibilityDto
}

class KtorReadingCompatibilityRemoteDataSource(
    private val httpClient: HttpClient,
) : ReadingCompatibilityRemoteDataSource {
    override suspend fun getCompatibility(targetUserId: String): ReadingCompatibilityDto =
        httpClient.get {
            url { appendPathSegments("users", targetUserId, "compatibility") }
        }.body()
}
