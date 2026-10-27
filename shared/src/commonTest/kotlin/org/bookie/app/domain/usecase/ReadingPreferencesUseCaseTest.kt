package org.bookie.app.domain.usecase

import kotlinx.coroutines.test.runTest
import org.bookie.app.domain.model.ReadingPreferences
import org.bookie.app.domain.model.ReadingPreferencesInput
import org.bookie.app.domain.repository.ReadingPreferencesRepository
import org.bookie.app.domain.result.Resource
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingPreferencesUseCaseTest {
    @Test
    fun `gets preferences through the repository`() = runTest {
        val expected = samplePreferences()
        val repository = FakeReadingPreferencesRepository(Resource.Success(expected))

        assertEquals(Resource.Success(expected), GetReadingPreferences(repository)())
        assertEquals(1, repository.getCalls)
    }

    @Test
    fun `saves preferences through the repository`() = runTest {
        val expected = samplePreferences()
        val input = ReadingPreferencesInput(
            preferredGenres = expected.preferredGenres,
            preferredAuthors = expected.preferredAuthors,
            favoriteBookIds = expected.favoriteBookIds,
        )
        val repository = FakeReadingPreferencesRepository(Resource.Success(expected))

        assertEquals(Resource.Success(expected), SaveReadingPreferences(repository)(input))
        assertEquals(input, repository.savedInput)
    }

    private class FakeReadingPreferencesRepository(
        private val result: Resource<ReadingPreferences>,
    ) : ReadingPreferencesRepository {
        var getCalls = 0
            private set
        var savedInput: ReadingPreferencesInput? = null
            private set

        override suspend fun getPreferences(): Resource<ReadingPreferences> {
            getCalls++
            return result
        }

        override suspend fun savePreferences(
            input: ReadingPreferencesInput,
        ): Resource<ReadingPreferences> {
            savedInput = input
            return result
        }
    }

    private fun samplePreferences() = ReadingPreferences(
        preferredGenres = listOf("Fantasy"),
        preferredAuthors = listOf("Ursula K. Le Guin"),
        favoriteBookIds = listOf("book-1"),
        onboardingCompleted = true,
    )
}
