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

    /**
     * Account-scoped models must never survive into another user's UI.
     * The shared HTTP client/cache remain singletons, but mutable ViewModel state does not.
     */
    fun createFeaturesForAccount(accountId: String): AccountFeatures {
        require(accountId.isNotBlank())
        return AccountFeatures(
            accountId = accountId,
            moodSearch = createAuthenticatedMoodSearchViewModel(clients),
            library = createAuthenticatedLibraryExperienceViewModel(clients, cache),
            preferences = createAuthenticatedReadingPreferencesViewModel(clients),
            stats = ReadingStatsViewModel(
                GetReadingStats(ReadingStatsRepositoryImpl(
                    KtorReadingStatsRemoteDataSource(clients.bookieClient),
                )),
            ),
        )
    }

    fun close() = clients.close()
}

/** Mutable authenticated view models are created once per signed-in account. */
internal data class AccountFeatures(
    val accountId: String,
    val moodSearch: MoodSearchViewModel,
    val library: LibraryExperienceViewModel,
    val preferences: ReadingPreferencesViewModel,
    val stats: ReadingStatsViewModel,
)

@Composable
internal expect fun rememberConfiguredAccountExperience(): ConfiguredAccountExperience?
