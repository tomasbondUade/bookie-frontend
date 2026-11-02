package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.appendPathSegments
import org.bookie.app.data.remote.dto.*

interface SeriesReadingRemoteDataSource {
    suspend fun getActiveSeries(): List<SeriesProgressDto>
    suspend fun getProgress(seriesId: String): SeriesProgressDto
    suspend fun getMap(seriesId: String, path: ReadingPathDto): SeriesReadingMapDto
}

class KtorSeriesReadingRemoteDataSource(
    private val client: HttpClient,
) : SeriesReadingRemoteDataSource {
    override suspend fun getActiveSeries(): List<SeriesProgressDto> =
        client.get("series/user/active").body()

    override suspend fun getProgress(seriesId: String): SeriesProgressDto {
        require(seriesId.isNotBlank()) { "El ID de la saga es obligatorio." }
        return client.get {
            url { appendPathSegments("series", seriesId, "progress") }
        }.body()
    }

    override suspend fun getMap(seriesId: String, path: ReadingPathDto): SeriesReadingMapDto {
        require(seriesId.isNotBlank()) { "El ID de la saga es obligatorio." }
        return client.get {
            url { appendPathSegments("series", seriesId, "map") }
            parameter("path", path.name)
        }.body()
    }
}
