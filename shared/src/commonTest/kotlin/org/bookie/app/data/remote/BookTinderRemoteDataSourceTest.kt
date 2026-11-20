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
import org.bookie.app.data.remote.dto.CreateSwipeDto
import org.bookie.app.data.remote.dto.SwipeTypeDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class BookTinderRemoteDataSourceTest {
    @Test
    fun `loads the authenticated feed with the requested limit`() = runTest {
        var captured: io.ktor.client.request.HttpRequestData? = null
        val client = createClient(AccessTokenProvider { "session-token" }) { request ->
            captured = request
            respond(FEED_JSON, headers = jsonHeaders())
        }

        val result = KtorBookTinderRemoteDataSource(client).getFeed(7)

        val request = assertNotNull(captured)
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("/v1/books/tinder", request.url.encodedPath)
        assertEquals("7", request.url.parameters["limit"])
        assertEquals("Bearer session-token", request.headers[HttpHeaders.Authorization])
        assertEquals("ai", result.source)
        assertEquals("book-1", result.books.single().id)
        assertEquals(listOf("Del autor favorito"), result.books.single().reasons)
        client.close()
    }

    @Test
    fun `records a swipe with only the internal book id and action`() = runTest {
        var captured: io.ktor.client.request.HttpRequestData? = null
        val client = createClient(AccessTokenProvider { "session-token" }) { request ->
            captured = request
            respond("""{"success":true,"message":"Swipe registrado"}""", HttpStatusCode.Created, jsonHeaders())
        }

        val result = KtorBookTinderRemoteDataSource(client).recordSwipe(
            CreateSwipeDto(bookId = "internal-book-id", type = SwipeTypeDto.LIKE),
        )

        val request = assertNotNull(captured)
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/v1/swipes", request.url.encodedPath)
        assertEquals("Bearer session-token", request.headers[HttpHeaders.Authorization])
        val body = (request.body as TextContent).text
        assertEquals("""{"bookId":"internal-book-id","type":"LIKE"}""", body)
        kotlin.test.assertTrue(result.success)
        client.close()
    }

    @Test
    fun `rejects invalid limits and blank book ids before making requests`() = runTest {
        var requests = 0
        val client = createClient {
            requests++
            respond(FEED_JSON, headers = jsonHeaders())
        }
        val remote = KtorBookTinderRemoteDataSource(client)

        assertFailsWith<IllegalArgumentException> { remote.getFeed(0) }
        assertFailsWith<IllegalArgumentException> { remote.getFeed(51) }
        assertFailsWith<IllegalArgumentException> {
            remote.recordSwipe(CreateSwipeDto("  ", SwipeTypeDto.DISLIKE))
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
        const val FEED_JSON = """{"source":"ai","count":1,"books":[{"id":"book-1","googleBookId":"google-1","title":"Dune","authors":["Frank Herbert"],"description":"A desert planet","genres":["Science fiction"],"coverUrl":null,"reasons":["Del autor favorito"]}]}"""
    }
}
