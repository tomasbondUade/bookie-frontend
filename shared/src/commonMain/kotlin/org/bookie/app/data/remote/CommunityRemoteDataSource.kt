package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.appendPathSegments
import org.bookie.app.data.remote.dto.CommunityDto
import org.bookie.app.data.remote.dto.CommunityPageDto
import org.bookie.app.data.remote.dto.CommunityPostDto

interface CommunityRemoteDataSource {
    suspend fun listCommunities(page: Int, limit: Int): CommunityPageDto
    suspend fun recommended(): List<CommunityDto>
    suspend fun posts(communityId: String, limit: Int): List<CommunityPostDto>
}

class KtorCommunityRemoteDataSource(
    private val httpClient: HttpClient,
) : CommunityRemoteDataSource {
    override suspend fun listCommunities(page: Int, limit: Int): CommunityPageDto {
        require(page >= 1) { "La página debe ser mayor que cero." }
        require(limit in 1..50) { "El límite debe estar entre 1 y 50." }
        return httpClient.get("communities") {
            parameter("page", page)
            parameter("limit", limit)
        }.body()
    }

    /** Protected endpoint. Caller must supply #8's session-aware Bookie client. */
    override suspend fun recommended(): List<CommunityDto> =
        httpClient.get("communities/recommended").body()

    override suspend fun posts(communityId: String, limit: Int): List<CommunityPostDto> {
        require(communityId.isNotBlank()) { "El ID de la comunidad es obligatorio." }
        require(limit in 1..50) { "El límite debe estar entre 1 y 50." }
        // Posts API supports a limit but does not currently expose page/offset.
        return httpClient.get {
            url { appendPathSegments("communities", communityId, "posts") }
            parameter("limit", limit)
        }.body()
    }
}
