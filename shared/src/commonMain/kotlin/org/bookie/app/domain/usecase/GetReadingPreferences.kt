package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.ReadingPreferences
import org.bookie.app.domain.repository.ReadingPreferencesRepository
import org.bookie.app.domain.result.Resource

class GetReadingPreferences(
    private val repository: ReadingPreferencesRepository,
) {
    suspend operator fun invoke(): Resource<ReadingPreferences> = repository.getPreferences()
}
