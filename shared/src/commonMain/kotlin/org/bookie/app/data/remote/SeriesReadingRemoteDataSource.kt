package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.appendPathSegments
import org.bookie.app.data.remote.dto.*

interface SeriesReadingRemoteDataSource {
    suspend fun getSeriesCatalog(page: Int, limit: Int, universeId: String?): SeriesCatalogPageDto
    suspend fun getUniverses(): List<UniverseCatalogDto>
    suspend fun getUniverseDetail(universeId: String): UniverseDetailDto
    suspend fun getActiveSeries(): List<SeriesProgressDto>
    suspend fun getProgress(seriesId: String): SeriesProgressDto
    suspend fun getMap(seriesId: String, path: ReadingPathDto): SeriesReadingMapDto
}

class KtorSeriesReadingRemoteDataSource(
    private val client: HttpClient,
) : SeriesReadingRemoteDataSource {
    override suspend fun getSeriesCatalog(
        page: Int,
        limit: Int,
        universeId: String?,
    ): SeriesCatalogPageDto {
        require(page >= 1) { "La página debe ser mayor que cero." }
        require(limit in 1..50) { "El límite debe estar entre 1 y 50." }
        return client.get("series") {
            parameter("page", page)
            parameter("limit", limit)
            universeId?.trim()?.takeIf(String::isNotEmpty)?.let { parameter("universeId", it) }
        }.body()
    }

    override suspend fun getUniverses(): List<UniverseCatalogDto> =
        client.get("universes").body()

    override suspend fun getUniverseDetail(universeId: String): UniverseDetailDto {
        require(universeId.isNotBlank()) { "El ID del universo es obligatorio." }
        return client.get {
            url { appendPathSegments("universes", universeId.trim()) }
        }.body()
    }

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
