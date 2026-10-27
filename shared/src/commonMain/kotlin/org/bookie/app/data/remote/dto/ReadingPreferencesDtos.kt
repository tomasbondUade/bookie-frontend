package org.bookie.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReadingPreferencesDto(
    val preferredGenres: List<String>,
    val preferredAuthors: List<String>,
    val favoriteBookIds: List<String>,
    val onboardingCompleted: Boolean,
)

@Serializable
data class SaveReadingPreferencesDto(
    val preferredGenres: List<String>,
    val preferredAuthors: List<String>,
    val favoriteBookIds: List<String>,
)
