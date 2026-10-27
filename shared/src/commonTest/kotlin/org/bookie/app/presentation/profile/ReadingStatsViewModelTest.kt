package org.bookie.app.presentation.profile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.bookie.app.domain.model.AverageRating
import org.bookie.app.domain.model.GenreReadingCount
import org.bookie.app.domain.model.ReadingStats
import org.bookie.app.domain.repository.ReadingStatsRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import org.bookie.app.domain.usecase.GetReadingStats
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingStatsViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads partial statistics without filling absent values with samples`() = runTest {
        val expected = ReadingStats(
            mostReadGenres = listOf(GenreReadingCount("Fantasy", 2)),
            averageRating = AverageRating(4.0, 1),
        )
        val viewModel = viewModel(Resource.Success(expected))

        viewModel.load()
        runCurrent()

        assertEquals(ReadingStatsUiState.Success(expected), viewModel.uiState.value)
    }

    @Test
    fun `shows an explicit empty state when the backend has no activity`() = runTest {
        val viewModel = viewModel(Resource.Success(ReadingStats()))

        viewModel.load()
        runCurrent()

        assertEquals(ReadingStatsUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `distinguishes an expired session from a recoverable error`() = runTest {
        val viewModel = viewModel(
            Resource.Error("HTTP 401", ResourceErrorKind.UNAUTHORIZED),
        )

        viewModel.load()
        runCurrent()

        assertEquals(
            ReadingStatsUiState.Error(
                message = "Tu sesión venció o no está disponible. Iniciá sesión nuevamente.",
                sessionExpired = true,
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `retries after a recoverable error`() = runTest {
        val repository = FakeRepository(
            mutableListOf(
                Resource.Error("Sin conexión"),
                Resource.Success(ReadingStats(mostReadGenres = listOf(GenreReadingCount("Fantasy", 1)))),
            ),
        )
        val viewModel = ReadingStatsViewModel(GetReadingStats(repository))

        viewModel.load()
        runCurrent()
        assertEquals(ReadingStatsUiState.Error("Sin conexión"), viewModel.uiState.value)

        viewModel.retry()
        runCurrent()

        assertEquals(2, repository.calls)
        assertEquals(
            ReadingStatsUiState.Success(
                ReadingStats(mostReadGenres = listOf(GenreReadingCount("Fantasy", 1))),
            ),
            viewModel.uiState.value,
        )
    }

    private fun viewModel(result: Resource<ReadingStats>): ReadingStatsViewModel =
        ReadingStatsViewModel(GetReadingStats(FakeRepository(mutableListOf(result))))

    private class FakeRepository(
        private val results: MutableList<Resource<ReadingStats>>,
    ) : ReadingStatsRepository {
        var calls = 0
            private set

        override suspend fun getStats(): Resource<ReadingStats> {
            calls++
            return results.removeAt(0)
        }
    }
}
