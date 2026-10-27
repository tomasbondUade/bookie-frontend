package org.bookie.app.data.mapper

import org.bookie.app.data.remote.dto.ReadingPreferencesDto
import org.bookie.app.data.remote.dto.SaveReadingPreferencesDto
import org.bookie.app.domain.model.ReadingPreferences
import org.bookie.app.domain.model.ReadingPreferencesInput

fun ReadingPreferencesDto.toDomain() = ReadingPreferences(
    preferredGenres = preferredGenres,
    preferredAuthors = preferredAuthors,
    favoriteBookIds = favoriteBookIds,
    onboardingCompleted = onboardingCompleted,
)

fun ReadingPreferencesInput.toDto() = SaveReadingPreferencesDto(
    preferredGenres = preferredGenres,
    preferredAuthors = preferredAuthors,
    favoriteBookIds = favoriteBookIds,
)
