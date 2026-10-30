package org.bookie.app.domain.usecase

import org.bookie.app.domain.repository.SeriesReadingRepository

class GetSeriesProgress(private val repository: SeriesReadingRepository) {
    suspend operator fun invoke(seriesId: String) = repository.getProgress(seriesId)
}
