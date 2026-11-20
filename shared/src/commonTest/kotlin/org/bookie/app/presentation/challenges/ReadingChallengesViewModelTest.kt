package org.bookie.app.presentation.challenges

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.bookie.app.domain.model.ReadingChallenge
import org.bookie.app.domain.model.ReadingChallengeProgress
import org.bookie.app.domain.repository.ReadingChallengesRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import org.bookie.app.domain.usecase.GetMyReadingChallengeProgress
import org.bookie.app.domain.usecase.GetReadingChallenges
import org.bookie.app.domain.usecase.JoinReadingChallenge
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingChallengesViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads catalog and current user's progress`() = runTest {
        val challenge = challenge(joined = true)
        val progress = progress()
        val repository = FakeRepository(
            challengeResults = mutableListOf(Resource.Success(listOf(challenge))),
            progressResults = mutableListOf(Resource.Success(listOf(progress))),
        )
        val viewModel = viewModel(repository)

        viewModel.load()
        runCurrent()

        assertEquals(
            ReadingChallengesUiState.Ready(listOf(challenge), listOf(progress)),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `joins once and refreshes joined state and progress only after server success`() = runTest {
        val repository = FakeRepository(
            challengeResults = mutableListOf(
                Resource.Success(listOf(challenge(joined = false))),
                Resource.Success(listOf(challenge(joined = true))),
            ),
            progressResults = mutableListOf(
                Resource.Success(emptyList()),
                Resource.Success(listOf(progress())),
            ),
            joinResults = mutableListOf(Resource.Success(Unit)),
        )
        val viewModel = viewModel(repository)

        viewModel.load()
        runCurrent()
        viewModel.join("challenge-1")
        viewModel.join("challenge-1")
        runCurrent()

        val state = viewModel.uiState.value as ReadingChallengesUiState.Ready
        assertEquals(listOf("challenge-1"), repository.joinedIds)
        assertEquals(true, state.challenges.single().joined)
        assertEquals(listOf(progress()), state.progress)
        assertEquals("Te uniste al reto.", state.message)
    }

    @Test
    fun `refreshes stale catalog after duplicate participation conflict`() = runTest {
        val repository = FakeRepository(
            challengeResults = mutableListOf(
                Resource.Success(listOf(challenge(joined = false))),
                Resource.Success(listOf(challenge(joined = true))),
            ),
            progressResults = mutableListOf(
                Resource.Success(emptyList()),
                Resource.Success(listOf(progress())),
            ),
            joinResults = mutableListOf(Resource.Error("HTTP 409", ResourceErrorKind.CONFLICT)),
        )
        val viewModel = viewModel(repository)

        viewModel.load()
        runCurrent()
        viewModel.join("challenge-1")
        runCurrent()

        val state = viewModel.uiState.value as ReadingChallengesUiState.Ready
        assertEquals(true, state.challenges.single().joined)
        assertEquals("Ya participás en este reto.", state.message)
    }

    @Test
    fun `requires sign in when a protected list request is unauthorized`() = runTest {
        val repository = FakeRepository(
            challengeResults = mutableListOf(Resource.Error("HTTP 401", ResourceErrorKind.UNAUTHORIZED)),
            progressResults = mutableListOf(),
        )
        val viewModel = viewModel(repository)

        viewModel.load()
        runCurrent()

        assertEquals(
            ReadingChallengesUiState.Error(
                "Tu sesión venció o no está disponible. Iniciá sesión nuevamente.",
                sessionExpired = true,
            ),
            viewModel.uiState.value,
        )
    }

    private fun viewModel(repository: FakeRepository) = ReadingChallengesViewModel(
        GetReadingChallenges(repository),
        GetMyReadingChallengeProgress(repository),
        JoinReadingChallenge(repository),
    )

    private fun challenge(joined: Boolean) = ReadingChallenge(
        id = "challenge-1",
        title = "Leer cinco libros de fantasía",
        description = "Leé cinco libros del género.",
        type = "GENRE",
        target = 5,
        genre = "Fantasy",
        createdAt = "2026-11-20T12:00:00.000Z",
        joined = joined,
    )

    private fun progress() = ReadingChallengeProgress(
        challengeId = "challenge-1",
        title = "Leer cinco libros de fantasía",
        description = "Leé cinco libros del género.",
        type = "GENRE",
        target = 5,
        genre = "Fantasy",
        completed = 2,
        percentage = 40.0,
        joinedAt = "2026-11-01T12:00:00.000Z",
    )

    private class FakeRepository(
        private val challengeResults: MutableList<Resource<List<ReadingChallenge>>>,
        private val progressResults: MutableList<Resource<List<ReadingChallengeProgress>>>,
        private val joinResults: MutableList<Resource<Unit>> = mutableListOf(Resource.Success(Unit)),
    ) : ReadingChallengesRepository {
        val joinedIds = mutableListOf<String>()

        override suspend fun getChallenges() = challengeResults.removeAt(0)
        override suspend fun getMyProgress() = progressResults.removeAt(0)
        override suspend fun joinChallenge(challengeId: String): Resource<Unit> {
            joinedIds += challengeId
            return joinResults.removeAt(0)
        }
    }
}
