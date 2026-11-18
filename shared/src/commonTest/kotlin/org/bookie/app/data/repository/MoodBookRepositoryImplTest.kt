package org.bookie.app.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.bookie.app.data.remote.BookieHttpClientConfig
import org.bookie.app.data.remote.KtorMoodBookRemoteDataSource
import org.bookie.app.data.remote.createBookieHttpClient
import org.bookie.app.domain.model.MoodBook
import org.bookie.app.domain.result.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MoodBookRepositoryImplTest {
    private fun jsonHeaders() = Headers.build {
        append(HttpHeaders.ContentType, "application/json")
    }

    @Test
    fun moodSearchUsesRealContractAndMapsRelevance() = runTest {
        var requestedUrl = ""
        val client = createBookieHttpClient(
            engine = MockEngine { request ->
                requestedUrl = request.url.toString()
                respond(
                    content = """
                        [{
                          "id":"book-1",
                          "googleBookId":"google-1",
                          "title":"Dune",
                          "authors":["Frank Herbert"],
                          "genres":["Ciencia ficción"],
                          "relevance":0.85
                        }]
                    """.trimIndent(),
                    headers = jsonHeaders(),
                )
            },
            config = BookieHttpClientConfig(baseUrl = "https://api.example.test"),
        )
        val repository = MoodBookRepositoryImpl(KtorMoodBookRemoteDataSource(client))
        val result = assertIs<Resource.Success<List<MoodBook>>>(
            repository.searchByMood("  aventura fantástica  "),
        )

        assertEquals(
            "https://api.example.test/books/mood?q=aventura+fant%C3%A1stica",
            requestedUrl,
        )
        assertEquals("Dune", result.data.single().book.title)
        assertEquals(0.85, result.data.single().relevance)
        client.close()
    }

    @Test
    fun emptyListAndBlankQueryDoNotCrash() = runTest {
        var count = 0
        val client = createBookieHttpClient(
            engine = MockEngine {
                count++
                respond("[]", headers = jsonHeaders())
            },
            config = BookieHttpClientConfig(baseUrl = "https://api.example.test"),
        )
        val repository = MoodBookRepositoryImpl(KtorMoodBookRemoteDataSource(client))

        assertEquals(Resource.Success(emptyList()), repository.searchByMood("nada"))
        assertEquals(Resource.Success(emptyList()), repository.searchByMood("   "))
        assertEquals(1, count)
        client.close()
    }

    @Test
    fun httpFailuresBecomeRepositoryErrors() = runTest {
        val client = createBookieHttpClient(
            engine = MockEngine {
                respond("unavailable", status = HttpStatusCode.ServiceUnavailable)
            },
            config = BookieHttpClientConfig(baseUrl = "https://api.example.test"),
        )
        val repository = MoodBookRepositoryImpl(KtorMoodBookRemoteDataSource(client))
        assertEquals(
            Resource.Error("El servidor respondió con HTTP 503."),
            repository.searchByMood("fantasía"),
        )
        client.close()
    }
}
