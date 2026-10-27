package org.bookie.app.domain.repository

import org.bookie.app.domain.model.ReadingStats
import org.bookie.app.domain.result.Resource

interface ReadingStatsRepository {
    suspend fun getStats(): Resource<ReadingStats>
}
