package org.bookie.app.data.repository

import org.bookie.app.data.mapper.toDomain
import org.bookie.app.data.mapper.toDto
import org.bookie.app.data.remote.ReadingPreferencesRemoteDataSource
import org.bookie.app.data.remote.toApiError
import org.bookie.app.data.remote.toMessage
import org.bookie.app.domain.model.ReadingPreferences
import org.bookie.app.domain.model.ReadingPreferencesInput
import org.bookie.app.domain.repository.ReadingPreferencesRepository
import org.bookie.app.domain.result.Resource

class ReadingPreferencesRepositoryImpl(
    private val remoteDataSource: ReadingPreferencesRemoteDataSource,
) : ReadingPreferencesRepository {
    override suspend fun getPreferences(): Resource<ReadingPreferences> = safely {
        remoteDataSource.getPreferences().toDomain()
    }

    override suspend fun savePreferences(
        input: ReadingPreferencesInput,
    ): Resource<ReadingPreferences> = safely {
        remoteDataSource.savePreferences(input.toDto()).toDomain()
    }

    private suspend fun <T> safely(block: suspend () -> T): Resource<T> = try {
        Resource.Success(block())
    } catch (exception: Exception) {
        Resource.Error(exception.toApiError().toMessage())
    }
}
