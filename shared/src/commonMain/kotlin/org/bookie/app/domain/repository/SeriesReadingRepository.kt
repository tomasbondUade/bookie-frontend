package org.bookie.app.domain.repository

import org.bookie.app.domain.model.*
import org.bookie.app.domain.result.Resource

/** Series exploration is public; personalized universe progress and reading data use the JWT. */
interface SeriesReadingRepository {
    suspend fun getSeriesCatalog(
        page: Int,
        limit: Int,
        universeId: String? = null,
    ): Resource<SeriesCatalogPage>
    suspend fun getUniverses(): Resource<List<UniverseCatalogEntry>>
    suspend fun getUniverseDetail(universeId: String): Resource<UniverseDetail>
    suspend fun getActiveSeries(): Resource<List<SeriesProgress>>
    suspend fun getProgress(seriesId: String): Resource<SeriesProgress>
    suspend fun getMap(seriesId: String, path: ReadingPath): Resource<SeriesReadingMap>
}
