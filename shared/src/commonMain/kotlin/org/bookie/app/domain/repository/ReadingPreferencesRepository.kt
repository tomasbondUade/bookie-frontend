package org.bookie.app.domain.repository

import org.bookie.app.domain.model.ReadingPreferences
import org.bookie.app.domain.model.ReadingPreferencesInput
import org.bookie.app.domain.result.Resource

interface ReadingPreferencesRepository {
    suspend fun getPreferences(): Resource<ReadingPreferences>

    suspend fun savePreferences(input: ReadingPreferencesInput): Resource<ReadingPreferences>
}
