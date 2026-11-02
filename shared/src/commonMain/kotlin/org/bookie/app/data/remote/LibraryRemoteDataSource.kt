package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import org.bookie.app.data.remote.dto.CreateReadingSessionDto
import org.bookie.app.data.remote.dto.LibraryPageDto
import org.bookie.app.data.remote.dto.LibraryBookStateDto
import org.bookie.app.data.remote.dto.LibraryStatusDto
import org.bookie.app.data.remote.dto.ReadingSessionDto
import org.bookie.app.data.remote.dto.SaveLibraryStateDto

interface LibraryRemoteDataSource {
    suspend fun getLibrary(
        page: Int,
        limit: Int,
        status: LibraryStatusDto?,
        favoritesOnly: Boolean,
    ): LibraryPageDto

    suspend fun deleteState(stateId: String)

    suspend fun saveState(input: SaveLibraryStateDto): LibraryBookStateDto

    /** The caller must reuse this key only when retrying the same operation. */
    suspend fun createReadingSession(
        stateId: String,
        input: CreateReadingSessionDto,
        idempotencyKey: String,
    ): ReadingSessionDto

    suspend fun getReadingSessions(stateId: String): List<ReadingSessionDto>
}

class KtorLibraryRemoteDataSource(
    private val httpClient: HttpClient,
) : LibraryRemoteDataSource {
    override suspend fun getLibrary(
        page: Int,
        limit: Int,
        status: LibraryStatusDto?,
        favoritesOnly: Boolean,
    ): LibraryPageDto {
        require(page >= 1) { "Page must be at least 1." }
        require(limit in 1..50) { "Limit must be between 1 and 50." }

        return httpClient.get("library") {
            parameter("page", page)
            parameter("limit", limit)
            status?.let { parameter("status", it.name) }
            if (favoritesOnly) parameter("filter", FAVORITES_FILTER)
        }.body()
    }

    override suspend fun deleteState(stateId: String) {
        require(stateId.isNotBlank()) { "Library state ID cannot be blank." }
        httpClient.delete {
            url { appendPathSegments("library", "state", stateId) }
        }
    }

    override suspend fun saveState(input: SaveLibraryStateDto): LibraryBookStateDto =
        httpClient.post("library/state") {
            contentType(ContentType.Application.Json)
            setBody(input)
        }.body()

    override suspend fun createReadingSession(
        stateId: String,
        input: CreateReadingSessionDto,
        idempotencyKey: String,
    ): ReadingSessionDto {
        require(stateId.isNotBlank()) { "Library state ID cannot be blank." }
        require(idempotencyKey.isNotBlank()) { "Idempotency key cannot be blank." }
        require(idempotencyKey.length <= MAX_IDEMPOTENCY_KEY_LENGTH) {
            "Idempotency key cannot exceed 255 characters."
        }

        return httpClient.post {
            url {
                appendPathSegments("library", "state", stateId, "sessions")
            }
            header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
            contentType(ContentType.Application.Json)
            setBody(input)
        }.body()
    }

    override suspend fun getReadingSessions(stateId: String): List<ReadingSessionDto> {
        require(stateId.isNotBlank()) { "Library state ID cannot be blank." }

        return httpClient.get {
            url {
                appendPathSegments("library", "state", stateId, "sessions")
            }
        }.body()
    }
}

private const val FAVORITES_FILTER = "fav"
private const val MAX_IDEMPOTENCY_KEY_LENGTH = 255
private const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"
