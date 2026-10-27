package org.bookie.app.domain.model

data class ReadingPreferences(
    val preferredGenres: List<String>,
    val preferredAuthors: List<String>,
    val favoriteBookIds: List<String>,
    val onboardingCompleted: Boolean,
)

data class ReadingPreferencesInput(
    val preferredGenres: List<String>,
    val preferredAuthors: List<String>,
    val favoriteBookIds: List<String> = emptyList(),
)
