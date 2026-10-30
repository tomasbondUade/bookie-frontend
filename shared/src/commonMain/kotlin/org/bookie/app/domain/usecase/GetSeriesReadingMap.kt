package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.ReadingPath
import org.bookie.app.domain.repository.SeriesReadingRepository

class GetSeriesReadingMap(private val repository: SeriesReadingRepository) {
    suspend operator fun invoke(seriesId: String, path: ReadingPath) = repository.getMap(seriesId, path)
}
