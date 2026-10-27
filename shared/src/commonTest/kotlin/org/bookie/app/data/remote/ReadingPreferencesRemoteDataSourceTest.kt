package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import org.bookie.app.data.remote.dto.SaveReadingPreferencesDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReadingPreferencesRemoteDataSourceTest {
    @Test
    fun `gets the authenticated user's preferences`() = runTest {
        var captured: io.ktor.client.request.HttpRequestData? = null
        val client = createClient(AccessTokenProvider { "session-token" }) { request ->
            captured = request
            respond(PREFERENCES_JSON, headers = jsonHeaders())
        }

        val result = KtorReadingPreferencesRemoteDataSource(client).getPreferences()

        val request = assertNotNull(captured)
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("/v1/users/preferences", request.url.encodedPath)
        assertEquals("Bearer session-token", request.headers[HttpHeaders.Authorization])
        assertEquals(listOf("Fantasy"), result.preferredGenres)
        assertEquals(listOf("Ursula K. Le Guin"), result.preferredAuthors)
        assertEquals(listOf("book-1"), result.favoriteBookIds)
        assertTrue(result.onboardingCompleted)
        client.close()
    }

    @Test
    fun `posts preferences without a user id and parses the server response`() = runTest {
        var captured: io.ktor.client.request.HttpRequestData? = null
        val client = createClient(AccessTokenProvider { "session-token" }) { request ->
            captured = request
            respond(
                """{"id":"user-1","email":"reader@example.test","username":"reader","avatarUrl":null,"preferredGenres":["Fantasy"],"preferredAuthors":["Ursula K. Le Guin"],"favoriteBookIds":["book-1"],"onboardingCompleted":true,"createdAt":"2026-01-01T00:00:00.000Z"}""",
                headers = jsonHeaders(),
            )
        }

        val result = KtorReadingPreferencesRemoteDataSource(client).savePreferences(
            SaveReadingPreferencesDto(
                preferredGenres = listOf("Fantasy"),
                preferredAuthors = listOf("Ursula K. Le Guin"),
                favoriteBookIds = listOf("book-1"),
            ),
        )

        val request = assertNotNull(captured)
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/v1/users/preferences", request.url.encodedPath)
        assertEquals("Bearer session-token", request.headers[HttpHeaders.Authorization])
        val body = (request.body as TextContent).text
        assertEquals(
            """{"preferredGenres":["Fantasy"],"preferredAuthors":["Ursula K. Le Guin"],"favoriteBookIds":["book-1"]}""",
            body,
        )
        assertTrue("userId" !in body)
        assertEquals(listOf("Fantasy"), result.preferredGenres)
        assertTrue(result.onboardingCompleted)
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
        const val PREFERENCES_JSON = """{"id":"user-1","username":"reader","preferredGenres":["Fantasy"],"preferredAuthors":["Ursula K. Le Guin"],"favoriteBookIds":["book-1"],"onboardingCompleted":true}"""
    }
}
