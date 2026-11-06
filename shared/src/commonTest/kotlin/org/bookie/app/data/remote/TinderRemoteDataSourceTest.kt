package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import org.bookie.app.data.remote.dto.CreateSwipeDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TinderRemoteDataSourceTest {
    private val headers = Headers.build { append(HttpHeaders.ContentType, "application/json") }

    @Test fun feedUsesAuthenticatedGet() = runTest {
        var request: io.ktor.client.request.HttpRequestData? = null
        val client = client { incoming ->
            request = incoming
            respond("""{"source":"fallback","count":0,"books":[]}""", headers = headers)
        }
        val result = KtorTinderRemoteDataSource(client).getFeed(10)
        val captured = assertNotNull(request)
        assertEquals(HttpMethod.Get, captured.method)
        assertEquals("/v1/books/tinder", captured.url.encodedPath)
        assertEquals("10", captured.url.parameters["limit"])
        assertEquals("Bearer session-token", captured.headers[HttpHeaders.Authorization])
        assertEquals(0, result.books.size)
        client.close()
    }

    @Test fun swipePostsBookIdAndTypeWithoutUserId() = runTest {
        var request: io.ktor.client.request.HttpRequestData? = null
        val client = client { incoming ->
            request = incoming
            respond("""{"success":true,"message":"Swipe registrado con éxito"}""", headers = headers)
        }
        val result = KtorTinderRemoteDataSource(client).sendSwipe(CreateSwipeDto("book-1", "LIKE"))
        val captured = assertNotNull(request)
        assertEquals(HttpMethod.Post, captured.method)
        assertEquals("/v1/swipes", captured.url.encodedPath)
        assertEquals("Bearer session-token", captured.headers[HttpHeaders.Authorization])
        val body = (captured.body as TextContent).text
        assertTrue(body.contains("book-1"))
        assertTrue(body.contains("LIKE"))
        assertTrue(!body.contains("userId"))
        assertTrue(result.success)
        client.close()
    }

    private fun client(
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData
    ): HttpClient = createBookieHttpClient(
        engine = MockEngine(handler),
        config = BookieHttpClientConfig(
            baseUrl = "https://api.example.test/v1",
            accessTokenProvider = AccessTokenProvider { "session-token" }
        )
    )
}
