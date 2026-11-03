package org.bookie.app.domain.usecase

import org.bookie.app.domain.repository.SeriesReadingRepository

class GetUniverseCatalog(private val repository: SeriesReadingRepository) {
    suspend operator fun invoke() = repository.getUniverses()
}
