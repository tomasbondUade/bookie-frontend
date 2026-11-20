package org.bookie.app

import androidx.compose.runtime.Composable
import org.bookie.app.data.auth.AuthClientBundle
import org.bookie.app.data.local.LibraryCache
import org.bookie.app.presentation.auth.AuthViewModel
import org.bookie.app.presentation.library.LibraryExperienceViewModel
import org.bookie.app.presentation.search.MoodSearchViewModel

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

    fun close() = clients.close()
}

@Composable
internal expect fun rememberConfiguredAccountExperience(): ConfiguredAccountExperience?
