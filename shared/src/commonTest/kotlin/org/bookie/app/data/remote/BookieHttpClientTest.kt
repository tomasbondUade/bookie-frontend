package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BookieHttpClientTest {
    @Test
    fun `uses configured base url and decodes json with unknown fields`() = runTest {
        var requestUrl = ""
        val client = createClient(MockEngine { request ->
            requestUrl = request.url.toString()
            respond(
                content = """{"name":"Ficciones","newField":true}""",
                headers = Headers.build { append(HttpHeaders.ContentType, "application/json") },
            )
        })

        val book: BookResponse = client.get("books/search?q=ficciones").body()

        assertEquals("https://api.example.test/v1/books/search?q=ficciones", requestUrl)
        assertEquals("Ficciones", book.name)
        client.close()
    }

    @Test
    fun `loads current token for each request and never sends it to another origin`() = runTest {
        var currentToken = "first-token"
        val authorizationHeaders = mutableListOf<String?>()
        val client = createClient(
            MockEngine { request ->
                authorizationHeaders += request.headers[HttpHeaders.Authorization]
                respond("")
            },
            tokenProvider = AccessTokenProvider { currentToken },
        )

        client.get("private")
        currentToken = "second-token"
        client.get("private")
        client.get("https://other.example.test/private") {
            header(HttpHeaders.Authorization, "Bearer caller-supplied")
        }

        assertEquals(
            listOf("Bearer first-token", "Bearer second-token", null),
            authorizationHeaders,
        )
        client.close()
    }

    @Test
    fun `validates error status and maps common transport errors`() = runTest {
        val client = createClient(
            MockEngine { respond("not found", status = HttpStatusCode.NotFound) },
        )

        val exception = assertFailsWith<ResponseException> {
            client.get("missing")
        }

        assertEquals(ApiError.Http(404), exception.toApiError())
        assertEquals(
            ApiError.Timeout,
            HttpRequestTimeoutException("https://api.example.test", 100L).toApiError(),
        )
        assertEquals(ApiError.Parsing, SerializationException("invalid json").toApiError())
        assertEquals(ApiError.Network, IOException("offline").toApiError())
        client.close()
    }

    @Test
    fun `maps malformed response bodies to parsing errors`() = runTest {
        val client = createClient(
            MockEngine {
                respond(
                    content = "{invalid-json}",
                    headers = Headers.build { append(HttpHeaders.ContentType, "application/json") },
                )
            },
        )

        val exception = assertFailsWith<JsonConvertException> {
            client.get("books").body<BookResponse>()
        }

        assertEquals(ApiError.Parsing, exception.toApiError())
        client.close()
    }

    @Test
    fun `does not translate coroutine cancellation into an API error`() {
        val cancellation = CancellationException("cancelled")

        assertFailsWith<CancellationException> {
            cancellation.toApiError()
        }
    }

    @Test
    fun `allows requests without an available token`() = runTest {
        var authorization: String? = "unexpected"
        val client = createClient(
            MockEngine { request ->
                authorization = request.headers[HttpHeaders.Authorization]
                respond("")
            },
            tokenProvider = AccessTokenProvider { null },
        )

        client.get("public") {
            header(HttpHeaders.Authorization, "Bearer caller-supplied")
        }

        assertNull(authorization)
        client.close()
    }

    @Test
    fun `requires a valid http base url and positive timeouts`() {
        assertFailsWith<IllegalArgumentException> {
            BookieHttpClientConfig(baseUrl = " ")
        }
        assertFailsWith<IllegalArgumentException> {
            BookieHttpClientConfig(baseUrl = "ftp://api.example.test")
        }
        assertFailsWith<IllegalArgumentException> {
            BookieHttpClientConfig(baseUrl = "https://user:password@api.example.test")
        }
        assertFailsWith<IllegalArgumentException> {
            BookieHttpClientConfig(baseUrl = "https://api.example.test?token=secret")
        }
        assertFailsWith<IllegalArgumentException> {
            HttpTimeouts(requestMillis = 0)
        }
    }

    private fun createClient(
        engine: MockEngine,
        tokenProvider: AccessTokenProvider? = null,
    ): HttpClient = createBookieHttpClient(
        engine = engine,
        config = BookieHttpClientConfig(
            baseUrl = "https://api.example.test/v1",
            accessTokenProvider = tokenProvider,
        ),
    )

    @Serializable
    private data class BookResponse(val name: String)
}
