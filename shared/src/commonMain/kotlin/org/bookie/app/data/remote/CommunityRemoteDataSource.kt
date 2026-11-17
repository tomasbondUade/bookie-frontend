package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.client.request.parameter
import org.bookie.app.data.remote.dto.CommunityDto
import org.bookie.app.data.remote.dto.CommunityPageDto
import org.bookie.app.data.remote.dto.CreateCommunityDto

interface CommunityRemoteDataSource {
    suspend fun listCommunities(page: Int, limit: Int): CommunityPageDto
    suspend fun recommended(): List<CommunityDto>
    suspend fun createCommunity(input: CreateCommunityDto): CommunityDto
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

    /** Auth is injected by the existing JWT-aware HttpClient, not supplied by the UI. */
    override suspend fun createCommunity(input: CreateCommunityDto): CommunityDto {
        require(input.name.isNotBlank() && input.description.isNotBlank()) {
            "El nombre y la descripción son obligatorios."
        }
        return httpClient.post("communities") {
            contentType(ContentType.Application.Json)
            setBody(input)
        }.body()
    }
}
