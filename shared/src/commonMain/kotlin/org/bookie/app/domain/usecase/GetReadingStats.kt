package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.ReadingStats
import org.bookie.app.domain.repository.ReadingStatsRepository
import org.bookie.app.domain.result.Resource

class GetReadingStats(
    private val repository: ReadingStatsRepository,
) {
    suspend operator fun invoke(): Resource<ReadingStats> = repository.getStats()
}
