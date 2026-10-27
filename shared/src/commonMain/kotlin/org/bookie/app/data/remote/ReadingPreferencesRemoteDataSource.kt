package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.bookie.app.data.remote.dto.ReadingPreferencesDto
import org.bookie.app.data.remote.dto.SaveReadingPreferencesDto

interface ReadingPreferencesRemoteDataSource {
    suspend fun getPreferences(): ReadingPreferencesDto

    suspend fun savePreferences(input: SaveReadingPreferencesDto): ReadingPreferencesDto
}

class KtorReadingPreferencesRemoteDataSource(
    private val httpClient: HttpClient,
) : ReadingPreferencesRemoteDataSource {
    override suspend fun getPreferences(): ReadingPreferencesDto =
        httpClient.get("users/preferences").body()

    override suspend fun savePreferences(
        input: SaveReadingPreferencesDto,
    ): ReadingPreferencesDto = httpClient.post("users/preferences") {
        contentType(ContentType.Application.Json)
        setBody(input)
    }.body()
}
