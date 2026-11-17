package org.bookie.app.data.repository

import org.bookie.app.data.remote.ApiError
import org.bookie.app.data.remote.CommunityRemoteDataSource
import org.bookie.app.data.remote.dto.CommunityDto
import org.bookie.app.data.remote.dto.CreateCommunityDto
import org.bookie.app.data.remote.toApiError
import org.bookie.app.data.remote.toMessage
import org.bookie.app.domain.model.Community
import org.bookie.app.domain.model.CommunityPage
import org.bookie.app.domain.model.CreateCommunityInput
import org.bookie.app.domain.repository.CommunityRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind

class CommunityRepositoryImpl(private val remote: CommunityRemoteDataSource) :
    CommunityRepository {

    override suspend fun listCommunities(page: Int, limit: Int): Resource<CommunityPage> =
        resource {
            val response = remote.listCommunities(page, limit)
            CommunityPage(
                communities = response.data.map(CommunityDto::toDomain),
                page = response.meta.page,
                limit = response.meta.limit,
                total = response.meta.total,
                totalPages = response.meta.totalPages,
            )
        }

    override suspend fun recommended(): Resource<List<Community>> =
        resource { remote.recommended().map(CommunityDto::toDomain) }

    override suspend fun createCommunity(input: CreateCommunityInput): Resource<Community> =
        resource {
            remote.createCommunity(
                CreateCommunityDto(
                    name = input.name.trim(),
                    description = input.description.trim(),
                ),
            ).toDomain()
        }


    private suspend fun <T> resource(action: suspend () -> T): Resource<T> =
        try {
            Resource.Success(action())
        } catch (error: Exception) {
            val apiError = error.toApiError()
            Resource.Error(
                message = apiError.toMessage(),
                kind = when (apiError) {
                    is ApiError.Http -> when (apiError.statusCode) {
                        400 -> ResourceErrorKind.VALIDATION
                        401 -> ResourceErrorKind.UNAUTHORIZED
                        403 -> ResourceErrorKind.FORBIDDEN
                        404 -> ResourceErrorKind.NOT_FOUND
                        409 -> ResourceErrorKind.CONFLICT
                        else -> ResourceErrorKind.GENERAL
                    }
                    ApiError.Network, ApiError.Timeout -> ResourceErrorKind.NETWORK
                    else -> ResourceErrorKind.GENERAL
                },
            )
        }
}

private fun CommunityDto.toDomain() = Community(
    id = id,
    name = name,
    description = description,
    tags = tags,
    imageUrl = imageUrl,
    ownerId = ownerId,
    createdAt = createdAt,
    score = score,
    matchedTags = matchedTags,
    reason = reason,
)
