package org.bookie.app.domain.usecase

import org.bookie.app.domain.repository.SeriesReadingRepository

class GetActiveSeries(private val repository: SeriesReadingRepository) {
    suspend operator fun invoke() = repository.getActiveSeries()
}
