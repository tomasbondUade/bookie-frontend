package org.bookie.app.data.mapper

import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import org.bookie.app.data.remote.dto.SeriesCatalogPageDto
import org.bookie.app.data.remote.dto.UniverseCatalogDto
import org.bookie.app.data.remote.dto.UniverseDetailDto
import org.bookie.app.domain.model.SeriesCatalogPage
import org.bookie.app.domain.model.UniverseCatalogEntry
import org.bookie.app.domain.model.UniverseDetail
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SeriesCatalogMapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `maps paged series and optional universe metadata`() {
        val dto = json.decodeFromString<SeriesCatalogPageDto>(SERIES_PAGE)

        assertEquals(
            SeriesCatalogPage(
                series = listOf(
                    org.bookie.app.domain.model.SeriesCatalogEntry(
                        id = "series-1",
                        universeId = "universe-1",
                        title = "Saga",
                        description = "Descripción",
                        coverUrl = null,
                        totalBooks = 3,
                        nodeCount = 4,
                        universe = org.bookie.app.domain.model.UniverseReference(
                            "universe-1", "Universo", null, null, "Autora",
                        ),
                    ),
                ),
                page = 2,
                limit = 20,
                total = 21,
                totalPages = 2,
            ),
            dto.toDomain(),
        )
    }

    @Test
    fun `maps personalized universe progress and defaults missing relation counts`() {
        val dto = json.decodeFromString<UniverseCatalogDto>(UNIVERSE)

        assertEquals(
            UniverseCatalogEntry(
                id = "universe-1",
                name = "Universo",
                description = null,
                imageUrl = null,
                author = "Autora",
                seriesCount = 2,
                progress = org.bookie.app.domain.model.UniverseProgress(
                    "universe-1", "Universo", 5, 2, 40.0, false,
                ),
            ),
            dto.toDomain(),
        )
        assertEquals(0, UniverseCatalogDto("id", "name").toDomain().seriesCount)
        assertNull(UniverseCatalogDto("id", "name").toDomain().progress)
    }

    @Test
    fun `maps universe detail series from nested nodes without coupling domain to node DTOs`() {
        val dto = json.decodeFromString<UniverseDetailDto>(UNIVERSE_DETAIL)
        val result = dto.toDomain()

        assertEquals(
            UniverseDetail(
                id = "universe-1",
                name = "Universo",
                description = "Descripción",
                imageUrl = "https://example.test/universe.jpg",
                author = "Autora",
                series = listOf(
                    org.bookie.app.domain.model.SeriesCatalogEntry(
                        id = "series-1",
                        universeId = "universe-1",
                        title = "Saga",
                        description = null,
                        coverUrl = null,
                        totalBooks = 1,
                        nodeCount = 1,
                        universe = null,
                    ),
                    org.bookie.app.domain.model.SeriesCatalogEntry(
                        id = "series-2",
                        universeId = null,
                        title = "Sin nodos",
                        description = null,
                        coverUrl = null,
                        totalBooks = 0,
                        nodeCount = 0,
                        universe = null,
                    ),
                ),
            ),
            result,
        )
    }

    private companion object {
        const val SERIES_PAGE = """{"data":[{"id":"series-1","universeId":"universe-1","title":"Saga","description":"Descripción","coverUrl":null,"totalBooks":3,"universe":{"id":"universe-1","name":"Universo","author":"Autora"},"_count":{"nodes":4}}],"meta":{"page":2,"limit":20,"total":21,"totalPages":2}}"""
        const val UNIVERSE = """{"id":"universe-1","name":"Universo","author":"Autora","_count":{"series":2},"progress":{"universeId":"universe-1","name":"Universo","totalBooks":5,"readBooks":2,"completionPercentage":40,"isCompleted":false}}"""
        const val UNIVERSE_DETAIL = """{"id":"universe-1","name":"Universo","description":"Descripción","imageUrl":"https://example.test/universe.jpg","author":"Autora","series":[{"id":"series-1","universeId":"universe-1","title":"Saga","totalBooks":1,"nodes":[{"id":"node-1","seriesId":"series-1","bookId":"book-1","nodeType":"MAIN","sequenceOrder":1,"book":{"id":"book-1","title":"Primer libro","authors":[],"genres":[]}}],"edges":[]},{"id":"series-2","title":"Sin nodos","totalBooks":0,"nodes":[],"edges":[]}]}"""
    }
}
