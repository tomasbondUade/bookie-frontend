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

    override suspend fun getComments(
        postId: String,
        limit: Int,
    ): Resource<List<CommunityComment>> =
        safely { remote.getComments(postId, limit).map { it.toDomain() } }

    override suspend fun createPost(
        communityId: String, input: CreateCommunityPostInput,
    ): Resource<CommunityPost> {
        if (tokenProvider.getAccessToken().isNullOrBlank()) {
            return Resource.Error("Iniciá sesión para publicar.", ResourceErrorKind.UNAUTHORIZED)
        }
        return safely { remote.createPost(communityId, input.toDto()).toDomain() }
    }

    override suspend fun createComment(
        postId: String,
        input: CreateCommunityCommentInput,
    ): Resource<CommunityComment> {
        if (tokenProvider.getAccessToken().isNullOrBlank()) {
            return Resource.Error("Iniciá sesión para comentar.", ResourceErrorKind.UNAUTHORIZED)
        }
        val content = input.content.trim()
        if (content.isEmpty() || content.length > 5000) {
            return Resource.Error(
                "El comentario debe tener entre 1 y 5000 caracteres.",
                ResourceErrorKind.VALIDATION,
            )
        }
        return safely { remote.createComment(postId, input.copy(content = content).toDto()).toDomain() }
    }

    private suspend fun <T> safely(block: suspend () -> T): Resource<T> = try {
        Resource.Success(block())
    } catch (error: Exception) {
        val apiError = error.toApiError()
        Resource.Error(
            error.toBookieErrorMessage(),
            apiError.toResourceKind(),
        )
    }

    private fun ApiError.toResourceKind(): ResourceErrorKind = when (this) {
        is ApiError.Http -> when (statusCode) {
            400, 422 -> ResourceErrorKind.VALIDATION
            401 -> ResourceErrorKind.UNAUTHORIZED
            403 -> ResourceErrorKind.FORBIDDEN
            404 -> ResourceErrorKind.NOT_FOUND
            409 -> ResourceErrorKind.CONFLICT
            else -> ResourceErrorKind.GENERAL
        }
        ApiError.Network, ApiError.Timeout -> ResourceErrorKind.NETWORK
        ApiError.Parsing, ApiError.Unknown -> ResourceErrorKind.GENERAL
    }
}
