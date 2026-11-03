package org.bookie.app.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.bookie.app.data.remote.AccessTokenProvider
import org.bookie.app.data.remote.BookieHttpClientConfig
import org.bookie.app.data.remote.KtorSeriesReadingRemoteDataSource
import org.bookie.app.data.remote.createBookieHttpClient
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SeriesCatalogRepositoryImplTest {
    @Test
    fun `requests a public paginated series page with only supported filters`() = runTest {
        var request: io.ktor.client.request.HttpRequestData? = null
        val client = createBookieHttpClient(MockEngine { captured ->
            request = captured
            respond(SERIES_PAGE, headers = jsonHeaders())
        }, BookieHttpClientConfig("https://api.example.test/v1"))
        val repository = SeriesReadingRepositoryImpl(KtorSeriesReadingRemoteDataSource(client))

        val page = assertIs<Resource.Success<org.bookie.app.domain.model.SeriesCatalogPage>>(
            repository.getSeriesCatalog(page = 3, limit = 10, universeId = "universe-1"),
        ).data

        assertEquals("/v1/series", request?.url?.encodedPath)
        assertEquals("3", request?.url?.parameters?.get("page"))
        assertEquals("10", request?.url?.parameters?.get("limit"))
        assertEquals("universe-1", request?.url?.parameters?.get("universeId"))
        assertEquals(null, request?.headers?.get(HttpHeaders.Authorization))
        assertEquals("Saga", page.series.single().title)
        assertEquals(3, page.totalPages)
        client.close()
    }

    @Test
    fun `requests authenticated universes and maps progress`() = runTest {
        var bearer: String? = null
        val client = createBookieHttpClient(MockEngine { request ->
            bearer = request.headers[HttpHeaders.Authorization]
            assertEquals("/v1/universes", request.url.encodedPath)
            respond(UNIVERSES, headers = jsonHeaders())
        }, BookieHttpClientConfig("https://api.example.test/v1", AccessTokenProvider { "session" }))
        val repository = SeriesReadingRepositoryImpl(KtorSeriesReadingRemoteDataSource(client))

        val universe = assertIs<Resource.Success<List<org.bookie.app.domain.model.UniverseCatalogEntry>>>(
            repository.getUniverses(),
        ).data.single()

        assertEquals("Bearer session", bearer)
        assertEquals(1, universe.seriesCount)
        assertEquals(1, universe.progress?.readBooks)
        client.close()
    }

    @Test
    fun `requests authenticated universe detail and maps nested saga node counts`() = runTest {
        val client = createBookieHttpClient(MockEngine { request ->
            assertEquals("/v1/universes/universe-1", request.url.encodedPath)
            assertEquals("Bearer jwt", request.headers[HttpHeaders.Authorization])
            respond(UNIVERSE_DETAIL, headers = jsonHeaders())
        }, BookieHttpClientConfig("https://api.example.test/v1", AccessTokenProvider { "jwt" }))
        val repository = SeriesReadingRepositoryImpl(KtorSeriesReadingRemoteDataSource(client))

        val detail = assertIs<Resource.Success<org.bookie.app.domain.model.UniverseDetail>>(
            repository.getUniverseDetail("universe-1"),
        ).data

        assertEquals("Universo", detail.name)
        assertEquals(1, detail.series.single().nodeCount)
        client.close()
    }

    @Test
    fun `maps authentication and authorization failures distinctly`() = runTest {
        val client = createBookieHttpClient(MockEngine { request ->
            if (request.url.encodedPath.endsWith("universes")) {
                respond("", HttpStatusCode.Unauthorized)
            } else {
                respond("", HttpStatusCode.Forbidden)
            }
        }, BookieHttpClientConfig("https://api.example.test/v1"))
        val repository = SeriesReadingRepositoryImpl(KtorSeriesReadingRemoteDataSource(client))

        assertEquals(
            ResourceErrorKind.UNAUTHORIZED,
            assertIs<Resource.Error>(repository.getUniverses()).kind,
        )
        assertEquals(
            ResourceErrorKind.FORBIDDEN,
            assertIs<Resource.Error>(repository.getUniverseDetail("universe-1")).kind,
        )
        client.close()
    }

    private fun jsonHeaders() = Headers.build {
        append(HttpHeaders.ContentType, "application/json")
    }

    private companion object {
        const val SERIES_PAGE = """{"data":[{"id":"series-1","universeId":"universe-1","title":"Saga","description":null,"coverUrl":null,"totalBooks":2,"universe":{"id":"universe-1","name":"Universo"},"_count":{"nodes":2}}],"meta":{"page":3,"limit":10,"total":30,"totalPages":3}}"""
        const val UNIVERSES = """[{"id":"universe-1","name":"Universo","_count":{"series":1},"progress":{"universeId":"universe-1","name":"Universo","totalBooks":2,"readBooks":1,"completionPercentage":50,"isCompleted":false}}]"""
        const val UNIVERSE_DETAIL = """{"id":"universe-1","name":"Universo","series":[{"id":"series-1","universeId":"universe-1","title":"Saga","totalBooks":1,"nodes":[{"id":"node-1","seriesId":"series-1","bookId":"book-1","nodeType":"MAIN","sequenceOrder":1,"book":{"id":"book-1","title":"Libro"}}],"edges":[]}] }"""
    }
}
