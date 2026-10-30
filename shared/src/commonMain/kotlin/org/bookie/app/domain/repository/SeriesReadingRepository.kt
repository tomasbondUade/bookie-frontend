package org.bookie.app.domain.repository

import org.bookie.app.domain.model.*
import org.bookie.app.domain.result.Resource

/** All endpoints are read-only and scoped to the authenticated JWT. */
interface SeriesReadingRepository {
    suspend fun getActiveSeries(): Resource<List<SeriesProgress>>
    suspend fun getProgress(seriesId: String): Resource<SeriesProgress>
    suspend fun getMap(seriesId: String, path: ReadingPath): Resource<SeriesReadingMap>
}
