package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingChallengesRemoteDataSourceTest {
    @Test
    fun `gets catalog and progress and joins with JWT and challenge id only`() = runTest {
        val requests = mutableListOf<io.ktor.client.request.HttpRequestData>()
        val client = createClient(AccessTokenProvider { "session-token" }) { request ->
            requests += request
            when (request.url.encodedPath) {
                "/v1/challenges" -> respond(CHALLENGES_JSON, headers = jsonHeaders())
                "/v1/challenges/my-progress" -> respond(PROGRESS_JSON, headers = jsonHeaders())
                "/v1/challenges/join" -> respond("{}", HttpStatusCode.Created, jsonHeaders())
                else -> error("Unexpected path: ${request.url.encodedPath}")
            }
        }
        val remote = KtorReadingChallengesRemoteDataSource(client)

        assertEquals(false, remote.getChallenges().single().joined)
        assertEquals(33.33, remote.getMyProgress().single().percentage)
        remote.joinChallenge("challenge-1")

        assertEquals(
            listOf("/v1/challenges", "/v1/challenges/my-progress", "/v1/challenges/join"),
            requests.map { it.url.encodedPath },
        )
        assertEquals(HttpMethod.Post, requests.last().method)
        assertEquals("Bearer session-token", requests.last().headers[HttpHeaders.Authorization])
        assertEquals(
            """{"challengeId":"challenge-1"}""",
            (requests.last().body as TextContent).text,
        )
        client.close()
    }

    @Test
    fun `rejects a blank challenge id without sending a request`() = runTest {
        var requests = 0
        val client = createClient {
            requests++
            respond("{}", HttpStatusCode.Created, jsonHeaders())
        }

        kotlin.test.assertFailsWith<IllegalArgumentException> {
            KtorReadingChallengesRemoteDataSource(client).joinChallenge(" ")
        }
        assertEquals(0, requests)
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
        const val CHALLENGES_JSON = """[{"id":"challenge-1","title":"Leer 12 libros","description":null,"type":"BOOK_COUNT","target":12,"genre":null,"createdAt":"2026-11-20T12:00:00.000Z","joined":false}]"""
        const val PROGRESS_JSON = """[{"challengeId":"challenge-1","title":"Leer 12 libros","description":null,"type":"BOOK_COUNT","target":12,"genre":null,"completed":4,"percentage":33.33,"joinedAt":"2026-11-01T12:00:00.000Z"}]"""
    }
}
