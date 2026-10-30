package org.bookie.app.data.repository

import org.bookie.app.data.mapper.toDomain
import org.bookie.app.data.mapper.toDto
import org.bookie.app.data.remote.*
import org.bookie.app.domain.model.*
import org.bookie.app.domain.repository.CommunityPostsRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind

class CommunityPostsRepositoryImpl(
    private val remote: CommunityPostsRemoteDataSource,
    private val tokenProvider: AccessTokenProvider,
) : CommunityPostsRepository {
    override suspend fun getPosts(communityId: String, limit: Int): Resource<List<CommunityPost>> =
        safely { remote.getPosts(communityId, limit).map { it.toDomain() } }

    override suspend fun createPost(
        communityId: String, input: CreateCommunityPostInput,
    ): Resource<CommunityPost> {
        if (tokenProvider.getAccessToken().isNullOrBlank()) {
            return Resource.Error("Iniciá sesión para publicar.", ResourceErrorKind.UNAUTHORIZED)
        }
        return safely { remote.createPost(communityId, input.toDto()).toDomain() }
    }

    private suspend fun <T> safely(block: suspend () -> T): Resource<T> = try {
        Resource.Success(block())
    } catch (error: Exception) {
        val apiError = error.toApiError()
        Resource.Error(
            apiError.toMessage(),
            if (apiError is ApiError.Http && apiError.statusCode == 401) ResourceErrorKind.UNAUTHORIZED
            else ResourceErrorKind.GENERAL,
        )
    }
}
