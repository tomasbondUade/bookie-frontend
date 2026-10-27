package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import org.bookie.app.data.remote.dto.ReadingStatsDto

interface ReadingStatsRemoteDataSource {
    suspend fun getStats(): ReadingStatsDto
}

class KtorReadingStatsRemoteDataSource(
    private val httpClient: HttpClient,
) : ReadingStatsRemoteDataSource {
    override suspend fun getStats(): ReadingStatsDto =
        httpClient.get("users/me/stats").body()
}
