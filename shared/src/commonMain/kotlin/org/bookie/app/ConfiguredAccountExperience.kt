package org.bookie.app

import androidx.compose.runtime.Composable
import org.bookie.app.data.auth.AuthClientBundle
import org.bookie.app.data.local.LibraryCache
import org.bookie.app.presentation.auth.AuthViewModel
import org.bookie.app.presentation.library.LibraryExperienceViewModel
import org.bookie.app.presentation.search.MoodSearchViewModel
import org.bookie.app.presentation.preferences.ReadingPreferencesViewModel
import org.bookie.app.presentation.profile.ReadingStatsViewModel
import org.bookie.app.data.remote.KtorReadingStatsRemoteDataSource
import org.bookie.app.data.repository.ReadingStatsRepositoryImpl
import org.bookie.app.domain.usecase.GetReadingStats

/**
 * One account-scoped composition root for authentication and protected API flows.
 * Uses the existing auth repository, client and SQLDelight cache.
 */
internal class ConfiguredAccountExperience(
    private val clients: AuthClientBundle,
    private val cache: LibraryCache,
) {
    val authViewModel: AuthViewModel = createAuthViewModel(clients, cache)

    fun moodSearchViewModel(): MoodSearchViewModel = createAuthenticatedMoodSearchViewModel(clients)

    fun libraryViewModel(): LibraryExperienceViewModel =
        createAuthenticatedLibraryExperienceViewModel(clients, cache)

    fun readingPreferencesViewModel(): ReadingPreferencesViewModel =
        createAuthenticatedReadingPreferencesViewModel(clients)

    fun readingStatsViewModel(): ReadingStatsViewModel = ReadingStatsViewModel(
        GetReadingStats(ReadingStatsRepositoryImpl(
            KtorReadingStatsRemoteDataSource(clients.bookieClient),
        )),
    )

    fun close() = clients.close()
}

@Composable
internal expect fun rememberConfiguredAccountExperience(): ConfiguredAccountExperience?
