package org.bookie.app.presentation.community

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.bookie.app.domain.model.CompatibilityUser
import org.bookie.app.domain.model.ReadingCompatibility
import org.bookie.app.domain.repository.ReadingCompatibilityRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import org.bookie.app.domain.usecase.GetReadingCompatibility
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingCompatibilityViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads compatibility for selected publication author`() = runTest {
        val expected = ReadingCompatibility(
            user = CompatibilityUser("user-2", "reader", null),
            compatibilityPercentage = 84,
            reasons = listOf("Comparten un libro"),
            dataQuality = "good",
        )
        val repository = FakeRepository(mutableListOf(Resource.Success(expected)))
        val viewModel = ReadingCompatibilityViewModel(GetReadingCompatibility(repository))

        viewModel.compareWith(" user-2 ")
        runCurrent()

        assertEquals(listOf("user-2"), repository.requestedIds)
        assertEquals(ReadingCompatibilityUiState.Ready("user-2", expected), viewModel.uiState.value)
    }

    @Test
    fun `shows session expiry separately and can retry`() = runTest {
        val repository = FakeRepository(
            mutableListOf(
                Resource.Error("HTTP 401", ResourceErrorKind.UNAUTHORIZED),
                Resource.Success(ReadingCompatibility(CompatibilityUser("user-2", null, null), 0)),
            ),
        )
        val viewModel = ReadingCompatibilityViewModel(GetReadingCompatibility(repository))

        viewModel.compareWith("user-2")
        runCurrent()
        assertEquals(
            ReadingCompatibilityUiState.Error(
                "user-2",
                "Tu sesión venció o no está disponible. Iniciá sesión nuevamente.",
                sessionExpired = true,
            ),
            viewModel.uiState.value,
        )

        viewModel.retry()
        runCurrent()

        assertEquals(2, repository.requestedIds.size)
        assertEquals("user-2", (viewModel.uiState.value as ReadingCompatibilityUiState.Ready).targetUserId)
    }

    private class FakeRepository(
        private val results: MutableList<Resource<ReadingCompatibility>>,
    ) : ReadingCompatibilityRepository {
        val requestedIds = mutableListOf<String>()

        override suspend fun getCompatibility(targetUserId: String): Resource<ReadingCompatibility> {
            requestedIds += targetUserId
            return results.removeAt(0)
        }
    }
}
