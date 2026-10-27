package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.ReadingPreferences
import org.bookie.app.domain.model.ReadingPreferencesInput
import org.bookie.app.domain.repository.ReadingPreferencesRepository
import org.bookie.app.domain.result.Resource

class SaveReadingPreferences(
    private val repository: ReadingPreferencesRepository,
) {
    suspend operator fun invoke(input: ReadingPreferencesInput): Resource<ReadingPreferences> =
        repository.savePreferences(input)
}
