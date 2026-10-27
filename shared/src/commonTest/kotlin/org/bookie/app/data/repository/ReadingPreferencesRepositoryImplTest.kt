package org.bookie.app.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.bookie.app.data.remote.BookieHttpClientConfig
import org.bookie.app.data.remote.KtorReadingPreferencesRemoteDataSource
import org.bookie.app.data.remote.ReadingPreferencesRemoteDataSource
import org.bookie.app.data.remote.createBookieHttpClient
import org.bookie.app.data.remote.dto.ReadingPreferencesDto
import org.bookie.app.data.remote.dto.SaveReadingPreferencesDto
import org.bookie.app.domain.model.ReadingPreferences
import org.bookie.app.domain.model.ReadingPreferencesInput
import org.bookie.app.domain.result.Resource
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingPreferencesRepositoryImplTest {
    @Test
    fun `maps the server response to the domain model`() = runTest {
        val repository = ReadingPreferencesRepositoryImpl(
            FakeRemoteDataSource(preferencesDto()),
        )

        assertEquals(Resource.Success(preferences()), repository.getPreferences())
    }

    @Test
    fun `maps input to the request and returns the confirmed server response`() = runTest {
        val input = ReadingPreferencesInput(
            preferredGenres = listOf("Fantasy"),
            preferredAuthors = listOf("Ursula K. Le Guin"),
            favoriteBookIds = listOf("book-2"),
        )
        val remote = FakeRemoteDataSource(preferencesDto(favoriteBookIds = listOf("book-2")))
        val repository = ReadingPreferencesRepositoryImpl(remote)

        assertEquals(Resource.Success(preferences(favoriteBookIds = listOf("book-2"))), repository.savePreferences(input))
        assertEquals(
            SaveReadingPreferencesDto(
                preferredGenres = input.preferredGenres,
                preferredAuthors = input.preferredAuthors,
                favoriteBookIds = input.favoriteBookIds,
            ),
            remote.savedInput,
        )
    }

    @Test
    fun `converts authentication failures to a resource error`() = runTest {
        val client = createBookieHttpClient(
            engine = MockEngine { respond("", HttpStatusCode.Unauthorized) },
            config = BookieHttpClientConfig(baseUrl = "https://api.example.test/v1"),
        )
        val repository = ReadingPreferencesRepositoryImpl(
            KtorReadingPreferencesRemoteDataSource(client),
        )

        assertEquals(Resource.Error("El servidor respondió con HTTP 401."), repository.getPreferences())
        client.close()
    }

    private class FakeRemoteDataSource(
        private val result: ReadingPreferencesDto,
    ) : ReadingPreferencesRemoteDataSource {
        var savedInput: SaveReadingPreferencesDto? = null
            private set

        override suspend fun getPreferences() = result

        override suspend fun savePreferences(input: SaveReadingPreferencesDto): ReadingPreferencesDto {
            savedInput = input
            return result
        }
    }

    private fun preferencesDto(favoriteBookIds: List<String> = listOf("book-1")) =
        ReadingPreferencesDto(
            preferredGenres = listOf("Fantasy"),
            preferredAuthors = listOf("Ursula K. Le Guin"),
            favoriteBookIds = favoriteBookIds,
            onboardingCompleted = true,
        )

    private fun preferences(favoriteBookIds: List<String> = listOf("book-1")) =
        ReadingPreferences(
            preferredGenres = listOf("Fantasy"),
            preferredAuthors = listOf("Ursula K. Le Guin"),
            favoriteBookIds = favoriteBookIds,
            onboardingCompleted = true,
        )
}
