package org.bookie.app

import androidx.compose.runtime.Composable
import org.bookie.app.data.auth.AuthClientBundle
import org.bookie.app.data.local.LibraryCache
import org.bookie.app.presentation.auth.AuthViewModel
import org.bookie.app.presentation.library.LibraryExperienceViewModel
import org.bookie.app.presentation.search.MoodSearchViewModel

/**
 * Connects the existing #27 auth client, #26 repository and platform cache,
 * without creating a second authentication implementation.
 */
internal class ConfiguredLibraryExperience(
    private val authClients: AuthClientBundle,
    private val cache: LibraryCache,
) {
    val authViewModel: AuthViewModel = createAuthViewModel(authClients)

    fun libraryViewModel(): LibraryExperienceViewModel =
        createAuthenticatedLibraryViewModel(authClients, cache)

    /**
     * Search reuses the SAME AuthClientBundle as Library: one Supabase
     * session, one JWT-aware backend client. Public discovery remains available,
     * while saving a book requires the actual signed-in session.
     */
    fun moodSearchViewModel(): MoodSearchViewModel =
        createAuthenticatedMoodSearchViewModel(authClients)

    fun close() = authClients.close()
}

/** Optional platform configuration; preview/public screens work without it. */
@Composable
internal expect fun rememberConfiguredLibraryExperience(): ConfiguredLibraryExperience?
