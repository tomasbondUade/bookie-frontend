package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import org.bookie.app.data.remote.dto.*

interface CommunityPostsRemoteDataSource {
    suspend fun getPosts(communityId: String, limit: Int): List<CommunityPostDto>
    suspend fun createPost(communityId: String, input: CreateCommunityPostDto): CommunityPostDto
}

class KtorCommunityPostsRemoteDataSource(private val client: HttpClient) : CommunityPostsRemoteDataSource {
    override suspend fun getPosts(communityId: String, limit: Int): List<CommunityPostDto> {
        require(communityId.isNotBlank()) { "La comunidad es obligatoria." }
        require(limit in 1..50) { "El límite debe estar entre 1 y 50." }
        return client.get {
            url { appendPathSegments("communities", communityId, "posts") }
            parameter("limit", limit)
        }.body()
    }

    override suspend fun createPost(communityId: String, input: CreateCommunityPostDto): CommunityPostDto {
        require(communityId.isNotBlank()) { "La comunidad es obligatoria." }
        require(input.title.isNotBlank() && input.content.isNotBlank()) { "Completá título y contenido." }
        return client.post {
            url { appendPathSegments("communities", communityId, "posts") }
            contentType(ContentType.Application.Json)
            setBody(input)
        }.body()
    }
}
