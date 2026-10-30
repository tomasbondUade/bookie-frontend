package org.bookie.app.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.bookie.app.data.remote.*
import org.bookie.app.domain.model.*
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import kotlin.test.*

class SeriesReadingRepositoryImplTest {
    private val progress = """{"seriesId":"series-1","title":"Saga","totalBooks":2,"readBooks":1,"completionPercentage":50.0,"isCompleted":false,"nodes":[{"nodeId":"node-1","bookId":"book-1","title":"Libro uno","nodeType":"MAIN","sequenceOrder":1.5,"status":"IN_PROGRESS","progressPercentage":30,"currentPage":120},{"nodeId":"node-2","bookId":"book-2","title":"Libro dos","nodeType":"MAIN","sequenceOrder":2,"status":"LOCKED"}]}"""
    private val map = """{"seriesId":"series-1","title":"Saga","path":"CHRONOLOGICAL","nodes":[{"nodeId":"node-2","bookId":"book-2","title":"Libro dos","nodeType":"MAIN","sequenceOrder":2,"publicationOrder":2,"chronologicalOrder":1,"recommendedOrder":2},{"nodeId":"node-1","bookId":"book-1","title":"Libro uno","nodeType":"MAIN","sequenceOrder":1.5,"publicationOrder":1,"chronologicalOrder":2,"recommendedOrder":1,"status":"IN_PROGRESS","currentPage":120,"progressPercentage":30}],"edges":[{"id":"edge-1","sourceNodeId":"node-1","targetNodeId":"node-2","relationType":"SEQUEL"}]}"""
    private fun headers() = Headers.build { append(HttpHeaders.ContentType, "application/json") }

    @Test fun `active series use JWT and map partial reading progress`() = runTest {
        var bearer = ""
        var path = ""
        val client = createBookieHttpClient(MockEngine { request ->
            bearer = request.headers[HttpHeaders.Authorization].orEmpty()
            path = request.url.encodedPath
            respond("[$progress]", headers = headers())
        }, BookieHttpClientConfig("https://api.example.test/v1", AccessTokenProvider { "jwt" }))
        val repo = SeriesReadingRepositoryImpl(KtorSeriesReadingRemoteDataSource(client))
        val result = assertIs<Resource.Success<List<SeriesProgress>>>(repo.getActiveSeries()).data
        assertEquals("/v1/series/user/active", path)
        assertEquals("Bearer jwt", bearer)
        assertEquals(50.0, result.single().completionPercentage)
        assertEquals(SeriesNodeStatus.IN_PROGRESS, result.single().nodes.first().status)
        assertEquals(120, result.single().nodes.first().currentPage)
        client.close()
    }

    @Test fun `reads individual progress without sending a user ID`() = runTest {
        val client = createBookieHttpClient(MockEngine { request ->
            assertEquals("/v1/series/series-1/progress", request.url.encodedPath)
            respond(progress, headers = headers())
        }, BookieHttpClientConfig("https://api.example.test/v1", AccessTokenProvider { "jwt" }))
        val result = SeriesReadingRepositoryImpl(KtorSeriesReadingRemoteDataSource(client)).getProgress("series-1")
        assertEquals(2, assertIs<Resource.Success<SeriesProgress>>(result).data.nodes.size)
        client.close()
    }

    @Test fun `chronological map keeps backend ordering, edges and optional progress`() = runTest {
        val client = createBookieHttpClient(MockEngine { request ->
            assertEquals("/v1/series/series-1/map", request.url.encodedPath)
            assertEquals("CHRONOLOGICAL", request.url.parameters["path"])
            respond(map, headers = headers())
        }, BookieHttpClientConfig("https://api.example.test/v1", AccessTokenProvider { "jwt" }))
        val result = assertIs<Resource.Success<SeriesReadingMap>>(
            SeriesReadingRepositoryImpl(KtorSeriesReadingRemoteDataSource(client))
                .getMap("series-1", ReadingPath.CHRONOLOGICAL),
        ).data
        assertEquals(ReadingPath.CHRONOLOGICAL, result.path)
        assertEquals(listOf("book-2", "book-1"), result.nodes.map { it.bookId })
        assertNull(result.nodes.first().status)
        assertEquals("node-2", result.edges.single().targetNodeId)
        client.close()
    }

    @Test fun `unauthorized response is mapped to session error`() = runTest {
        val client = createBookieHttpClient(MockEngine { respond("", HttpStatusCode.Unauthorized) },
            BookieHttpClientConfig("https://api.example.test/v1", AccessTokenProvider { "expired" }))
        val result = SeriesReadingRepositoryImpl(KtorSeriesReadingRemoteDataSource(client)).getActiveSeries()
        assertEquals(ResourceErrorKind.UNAUTHORIZED, assertIs<Resource.Error>(result).kind)
        client.close()
    }

    @Test fun `invalid saga ID is rejected before HTTP`() = runTest {
        var calls = 0
        val client = createBookieHttpClient(MockEngine { calls++; respond("{}") },
            BookieHttpClientConfig("https://api.example.test/v1"))
        val result = SeriesReadingRepositoryImpl(KtorSeriesReadingRemoteDataSource(client)).getProgress(" ")
        assertIs<Resource.Error>(result)
        assertEquals(0, calls)
        client.close()
    }
}
