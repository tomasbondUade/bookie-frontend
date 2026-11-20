package org.bookie.app.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.bookie.app.data.remote.AccessTokenProvider
import org.bookie.app.data.remote.BookieHttpClientConfig
import org.bookie.app.data.remote.KtorReadingChallengesRemoteDataSource
import org.bookie.app.data.remote.ReadingChallengesRemoteDataSource
import org.bookie.app.data.remote.createBookieHttpClient
import org.bookie.app.data.remote.dto.ReadingChallengeDto
import org.bookie.app.data.remote.dto.ReadingChallengeProgressDto
import org.bookie.app.domain.model.ReadingChallenge
import org.bookie.app.domain.model.ReadingChallengeProgress
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingChallengesRepositoryImplTest {
    @Test
    fun `maps both server responses to domain models`() = runTest {
        val repository = ReadingChallengesRepositoryImpl(
            FakeRemoteDataSource(),
            AccessTokenProvider { "session-token" },
        )

        assertEquals(
            Resource.Success(listOf(ReadingChallenge("challenge-1", "Leer", null, "BOOK_COUNT", 3, null, "now", false))),
            repository.getChallenges(),
        )
        assertEquals(
            Resource.Success(listOf(ReadingChallengeProgress("challenge-1", "Leer", null, "BOOK_COUNT", 3, null, 1, 33.0, "now"))),
            repository.getMyProgress(),
        )
        assertEquals(Resource.Success(Unit), repository.joinChallenge("challenge-1"))
    }

    @Test
    fun `refuses protected routes without a session`() = runTest {
        var requests = 0
        val remote = object : ReadingChallengesRemoteDataSource by FakeRemoteDataSource() {
            override suspend fun getChallenges(): List<ReadingChallengeDto> {
                requests++
                return emptyList()
            }
        }
        val repository = ReadingChallengesRepositoryImpl(remote, AccessTokenProvider { null })

        assertEquals(
            Resource.Error("Iniciá sesión para consultar y unirte a los retos.", ResourceErrorKind.UNAUTHORIZED),
            repository.getChallenges(),
        )
        assertEquals(0, requests)
    }

    @Test
    fun `maps duplicate participation to a conflict for the presentation layer`() = runTest {
        val client = createBookieHttpClient(
            engine = MockEngine { respond("", HttpStatusCode.Conflict) },
            config = BookieHttpClientConfig(
                baseUrl = "https://api.example.test/v1",
                accessTokenProvider = AccessTokenProvider { "session-token" },
            ),
        )
        val repository = ReadingChallengesRepositoryImpl(
            KtorReadingChallengesRemoteDataSource(client),
            AccessTokenProvider { "session-token" },
        )

        val result = repository.joinChallenge("challenge-1")

        assertEquals(ResourceErrorKind.CONFLICT, (result as Resource.Error).kind)
        client.close()
    }

    private class FakeRemoteDataSource : ReadingChallengesRemoteDataSource {
        override suspend fun getChallenges() = listOf(
            ReadingChallengeDto("challenge-1", "Leer", type = "BOOK_COUNT", target = 3, createdAt = "now", joined = false),
        )
        override suspend fun getMyProgress() = listOf(
            ReadingChallengeProgressDto("challenge-1", "Leer", type = "BOOK_COUNT", target = 3, completed = 1, percentage = 33.0, joinedAt = "now"),
        )
        override suspend fun joinChallenge(challengeId: String) = Unit
    }
}
