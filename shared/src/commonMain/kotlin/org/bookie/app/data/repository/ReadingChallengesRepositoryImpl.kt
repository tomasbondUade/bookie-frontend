package org.bookie.app.data.repository

import org.bookie.app.data.mapper.toDomain
import org.bookie.app.data.remote.AccessTokenProvider
import org.bookie.app.data.remote.ApiError
import org.bookie.app.data.remote.ReadingChallengesRemoteDataSource
import org.bookie.app.data.remote.toApiError
import org.bookie.app.data.remote.toMessage
import org.bookie.app.domain.model.ReadingChallenge
import org.bookie.app.domain.model.ReadingChallengeProgress
import org.bookie.app.domain.repository.ReadingChallengesRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind

class ReadingChallengesRepositoryImpl(
    private val remote: ReadingChallengesRemoteDataSource,
    private val tokenProvider: AccessTokenProvider,
) : ReadingChallengesRepository {
    override suspend fun getChallenges(): Resource<List<ReadingChallenge>> = safely {
        remote.getChallenges().map { it.toDomain() }
    }

    override suspend fun getMyProgress(): Resource<List<ReadingChallengeProgress>> = safely {
        remote.getMyProgress().map { it.toDomain() }
    }

    override suspend fun joinChallenge(challengeId: String): Resource<Unit> = safely {
        remote.joinChallenge(challengeId)
    }

    private suspend fun <T> safely(block: suspend () -> T): Resource<T> = try {
        if (tokenProvider.getAccessToken().isNullOrBlank()) {
            return Resource.Error(
                "Iniciá sesión para consultar y unirte a los retos.",
                ResourceErrorKind.UNAUTHORIZED,
            )
        }
        Resource.Success(block())
    } catch (exception: Exception) {
        val error = exception.toApiError()
        Resource.Error(error.toMessage(), error.toResourceKind())
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
