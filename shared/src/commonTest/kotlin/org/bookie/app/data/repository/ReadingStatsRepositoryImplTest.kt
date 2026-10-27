package org.bookie.app.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.bookie.app.data.remote.BookieHttpClientConfig
import org.bookie.app.data.remote.KtorReadingStatsRemoteDataSource
import org.bookie.app.data.remote.ReadingStatsRemoteDataSource
import org.bookie.app.data.remote.createBookieHttpClient
import org.bookie.app.data.remote.dto.AverageRatingDto
import org.bookie.app.data.remote.dto.ReadingStatsDto
import org.bookie.app.domain.model.AverageRating
import org.bookie.app.domain.model.ReadingStats
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingStatsRepositoryImplTest {
    @Test
    fun `maps server response to domain statistics`() = runTest {
        val repository = ReadingStatsRepositoryImpl(
            FakeRemoteDataSource(ReadingStatsDto(averageRating = AverageRatingDto(4.5, 8))),
        )

        assertEquals(
            Resource.Success(ReadingStats(averageRating = AverageRating(4.5, 8))),
            repository.getStats(),
        )
    }

    @Test
    fun `marks unauthorized responses so presentation can ask the user to sign in`() = runTest {
        val client = createBookieHttpClient(
            engine = MockEngine { respond("", HttpStatusCode.Unauthorized) },
            config = BookieHttpClientConfig(baseUrl = "https://api.example.test/v1"),
        )
        val repository = ReadingStatsRepositoryImpl(KtorReadingStatsRemoteDataSource(client))

        assertEquals(
            Resource.Error(
                message = "El servidor respondió con HTTP 401.",
                kind = ResourceErrorKind.UNAUTHORIZED,
            ),
            repository.getStats(),
        )
        client.close()
    }

    private class FakeRemoteDataSource(
        private val result: ReadingStatsDto,
    ) : ReadingStatsRemoteDataSource {
        override suspend fun getStats() = result
    }
}
