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

class ReadingCompatibilityRemoteDataSourceTest {
    @Test
    fun `requests compatibility for target user through the authenticated client`() = runTest {
        var captured: io.ktor.client.request.HttpRequestData? = null
        val client = createClient(AccessTokenProvider { "session-token" }) { request ->
            captured = request
            respond(COMPATIBILITY_JSON, headers = jsonHeaders())
        }

        val result = KtorReadingCompatibilityRemoteDataSource(client).getCompatibility("user-2")

        val request = assertNotNull(captured)
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("/v1/users/user-2/compatibility", request.url.encodedPath)
        assertEquals("Bearer session-token", request.headers[HttpHeaders.Authorization])
        assertEquals(84, result.compatibilityPercentage)
        assertEquals("book-1", result.sharedBooks.first().id)
        assertEquals("good", result.dataQuality)
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
        const val COMPATIBILITY_JSON = """{"user":{"id":"user-2","username":"reader","avatarUrl":null},"compatibilityPercentage":84,"sharedBooks":[{"id":"book-1","title":"The Hobbit"}],"sharedRatings":[{"bookId":"book-1","title":"The Hobbit","userRating":5,"targetRating":4}],"sharedGenres":["Fantasy"],"sharedAuthors":["Tolkien"],"reasons":["Comparten el libro The Hobbit"],"dataQuality":"good"}"""
    }
}
