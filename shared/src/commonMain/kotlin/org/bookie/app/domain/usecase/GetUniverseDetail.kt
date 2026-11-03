package org.bookie.app.domain.usecase

import org.bookie.app.domain.repository.SeriesReadingRepository

class GetUniverseDetail(private val repository: SeriesReadingRepository) {
    suspend operator fun invoke(universeId: String) = repository.getUniverseDetail(universeId)
}
