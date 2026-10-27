package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ReadingStatsRemoteDataSourceTest {
    @Test
    fun `gets the authenticated user's statistics without a manual user id`() = runTest {
        var captured: io.ktor.client.request.HttpRequestData? = null
        val client = createClient(AccessTokenProvider { "session-token" }) { request ->
            captured = request
            respond(STATS_JSON, headers = jsonHeaders())
        }

        val result = KtorReadingStatsRemoteDataSource(client).getStats()

        val request = assertNotNull(captured)
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("/v1/users/me/stats", request.url.encodedPath)
        assertEquals("Bearer session-token", request.headers[HttpHeaders.Authorization])
        assertEquals(emptySet(), request.url.parameters.names())
        assertEquals("Fantasy", result.mostReadGenres.first().genre)
        assertEquals(4.5, result.averageRating.average)
        assertEquals(5, result.readingStreak.longestStreak)
        client.close()
    }

    @Test
    fun `accepts an empty response for users without statistics`() = runTest {
        val client = createClient { respond("{}", headers = jsonHeaders()) }

        val result = KtorReadingStatsRemoteDataSource(client).getStats()

        assertEquals(emptyList(), result.mostReadGenres)
        assertEquals(emptyList(), result.booksReadByMonth)
        assertEquals(0.0, result.averageRating.average)
        assertEquals(0, result.readingStreak.currentStreak)
        client.close()
    }

    private fun createClient(
        tokenProvider: AccessTokenProvider? = null,
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData,
    ): HttpClient = createBookieHttpClient(
        engine = MockEngine(handler),
        config = BookieHttpClientConfig(
            baseUrl = "https://api.example.test/v1",
            accessTokenProvider = tokenProvider,
        ),
    )

    private fun jsonHeaders() = Headers.build {
        append(HttpHeaders.ContentType, "application/json")
    }

    private companion object {
        const val STATS_JSON = """{"mostReadGenres":[{"genre":"Fantasy","count":6}],"favoriteAuthors":[{"author":"Ursula K. Le Guin","count":2}],"averageRating":{"average":4.5,"count":8},"booksReadByMonth":[{"year":2026,"month":9,"count":3}],"booksReadByYear":[{"year":2026,"count":7}],"readingStreak":{"currentStreak":3,"longestStreak":5}}"""
    }
}
