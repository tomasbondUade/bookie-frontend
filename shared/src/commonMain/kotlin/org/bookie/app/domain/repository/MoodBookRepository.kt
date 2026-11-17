package org.bookie.app.domain.repository

import org.bookie.app.domain.model.MoodBook
import org.bookie.app.domain.result.Resource

interface MoodBookRepository {
    suspend fun searchByMood(query: String): Resource<List<MoodBook>>
}
