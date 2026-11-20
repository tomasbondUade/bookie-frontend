package org.bookie.app.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.bookie.app.data.remote.BookieHttpClientConfig
import org.bookie.app.data.remote.KtorBookTinderRemoteDataSource
import org.bookie.app.data.remote.createBookieHttpClient
import org.bookie.app.domain.model.RecommendationSource
import org.bookie.app.domain.model.SwipeType
import org.bookie.app.domain.model.BookTinderFeed
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BookTinderRepositoryImplTest {
    @Test
    fun `maps recommendation source reasons and books into domain`() = runTest {
        val repository = repository { request ->
            assertEquals("/books/tinder", request.url.encodedPath)
            respond(FEED_JSON, headers = jsonHeaders())
        }

        val result = assertIs<Resource.Success<BookTinderFeed>>(repository.getFeed())
        val feed = result.data
        assertEquals(RecommendationSource.AI, feed.source)
        assertEquals(1, feed.count)
        assertEquals("Dune", feed.books.single().book.title)
        assertEquals(listOf("Del autor favorito"), feed.books.single().reasons)
    }

    @Test
    fun `uses fallback source and maps expired sessions`() = runTest {
        val fallback = repository {
            respond(FEED_JSON.replace("\"ai\"", "\"fallback\""), headers = jsonHeaders())
        }
        val result = assertIs<Resource.Success<BookTinderFeed>>(fallback.getFeed())
        assertEquals(
            RecommendationSource.FALLBACK,
            result.data.source,
        )

        val expired = repository { respond("{}", HttpStatusCode.Unauthorized, jsonHeaders()) }
        val error = assertIs<Resource.Error>(expired.recordSwipe("book-1", SwipeType.LIKE))
        assertEquals(ResourceErrorKind.UNAUTHORIZED, error.kind)
    }

    @Test
    fun `requires a successful response before confirming the swipe`() = runTest {
        val accepted = repository { respond("""{"success":true}""", HttpStatusCode.Created, jsonHeaders()) }
        assertEquals(Resource.Success(Unit), accepted.recordSwipe("book-1", SwipeType.DISLIKE))

        val rejected = repository { respond("""{"success":false,"message":"not saved"}""", headers = jsonHeaders()) }
        assertIs<Resource.Error>(rejected.recordSwipe("book-1", SwipeType.LIKE))
    }

    private fun repository(
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData,
    ): BookTinderRepositoryImpl = BookTinderRepositoryImpl(
        KtorBookTinderRemoteDataSource(
            createBookieHttpClient(
                engine = MockEngine(handler),
                config = BookieHttpClientConfig(baseUrl = "https://api.example.test"),
            ),
        ),
    )

    private fun jsonHeaders() = Headers.build {
        append(HttpHeaders.ContentType, "application/json")
    }

    private companion object {
        const val FEED_JSON = """{"source":"ai","count":1,"books":[{"id":"book-1","googleBookId":"google-1","title":"Dune","authors":["Frank Herbert"],"genres":["Science fiction"],"reasons":["Del autor favorito"]}]}"""
    }
}
