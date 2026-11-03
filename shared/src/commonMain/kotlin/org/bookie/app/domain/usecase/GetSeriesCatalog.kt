package org.bookie.app.domain.usecase

import org.bookie.app.domain.repository.SeriesReadingRepository

class GetSeriesCatalog(private val repository: SeriesReadingRepository) {
    suspend operator fun invoke(page: Int, limit: Int, universeId: String? = null) =
        repository.getSeriesCatalog(page, limit, universeId)
}
