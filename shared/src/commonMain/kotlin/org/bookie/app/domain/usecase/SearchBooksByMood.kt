package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.MoodBook
import org.bookie.app.domain.repository.MoodBookRepository
import org.bookie.app.domain.result.Resource

class SearchBooksByMood(private val repository: MoodBookRepository) {
    suspend operator fun invoke(query: String): Resource<List<MoodBook>> =
        repository.searchByMood(query)
}
