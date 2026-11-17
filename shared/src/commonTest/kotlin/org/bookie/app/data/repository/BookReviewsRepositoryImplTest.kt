package org.bookie.app.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.bookie.app.data.remote.BookieHttpClientConfig
import org.bookie.app.data.remote.KtorBookReviewsRemoteDataSource
import org.bookie.app.data.remote.createBookieHttpClient
import org.bookie.app.domain.model.BookReviewSort
import org.bookie.app.domain.result.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BookReviewsRepositoryImplTest {
    @Test
    fun parsesPageAndUsesInternalBookIdAndSort() = runTest {
        var requestedPath = ""
        var requestedPage: String? = null
        var requestedLimit: String? = null
        var requestedSort: String? = null
        val client = createBookieHttpClient(
            MockEngine { request ->
                requestedPath = request.url.encodedPath
                requestedPage = request.url.parameters["page"]
                requestedLimit = request.url.parameters["limit"]
                requestedSort = request.url.parameters["sort"]
                respond(
                    content = """{
                        "data": [{
                            "id": "review-1",
                            "rating": 5,
                            "comment": " Muy recomendado ",
                            "updatedAt": "2026-11-17T10:30:00.000Z",
                            "user": {"id": "user-1", "username": "Lectora", "avatarUrl": null}
                        }, {
                            "id": "review-2",
                            "rating": null,
                            "comment": "Lectura entretenida",
                            "updatedAt": "2026-11-16T09:00:00.000Z",
                            "user": {"id": "user-2", "username": "Usuario", "avatarUrl": null}
                        }],
                        "meta": {"page": 2, "limit": 10, "total": 12, "totalPages": 2}
                    }""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
            BookieHttpClientConfig("https://bookie.example.test"),
        )
        try {
            val repo = BookReviewsRepositoryImpl(KtorBookReviewsRemoteDataSource(client))
            val page = assertIs<Resource.Success<*>>(
                repo.getReviews("book-123", 2, 10, BookReviewSort.TOP),
            ).data as org.bookie.app.domain.model.BookReviewsPage

            assertEquals("/books/book-123/reviews", requestedPath)
            assertEquals("2", requestedPage)
            assertEquals("10", requestedLimit)
            assertEquals("top", requestedSort)
            assertEquals("Lectora", page.reviews.first().username)
            assertEquals(5, page.reviews.first().rating)
            assertEquals("Muy recomendado", page.reviews.first().comment)
            assertEquals(null, page.reviews[1].rating)
            assertEquals(2, page.page)
            assertEquals(12, page.total)
        } finally { client.close() }
    }

    @Test
    fun emptyResponseMapsToEmptyPage() = runTest {
        val client = createBookieHttpClient(
            MockEngine {
                respond(
                    content = """{"data":[],"meta":{"page":1,"limit":10,"total":0,"totalPages":0}}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
            BookieHttpClientConfig("https://bookie.example.test"),
        )
        try {
            val repo = BookReviewsRepositoryImpl(KtorBookReviewsRemoteDataSource(client))
            val result = assertIs<Resource.Success<*>>(repo.getReviews("book-123"))
                .data as org.bookie.app.domain.model.BookReviewsPage
            assertEquals(emptyList(), result.reviews)
            assertEquals(0, result.total)
        } finally { client.close() }
    }

    @Test
    fun http404IsReportedAsErrorNotFakeReviews() = runTest {
        val client = createBookieHttpClient(
            MockEngine { respond("Missing book", status = HttpStatusCode.NotFound) },
            BookieHttpClientConfig("https://bookie.example.test"),
        )
        try {
            val repo = BookReviewsRepositoryImpl(KtorBookReviewsRemoteDataSource(client))
            val result = assertIs<Resource.Error>(repo.getReviews("missing-book"))
            assertTrue(result.message.contains("404"))
        } finally { client.close() }
    }
}
