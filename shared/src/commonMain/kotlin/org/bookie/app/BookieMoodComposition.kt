package org.bookie.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import org.bookie.app.data.remote.BookieHttpClientConfig
import org.bookie.app.data.remote.KtorLibraryRemoteDataSource
import org.bookie.app.data.remote.KtorMoodBookRemoteDataSource
import org.bookie.app.data.remote.createPlatformHttpClient
import org.bookie.app.data.repository.LibraryRepositoryImpl
import org.bookie.app.data.repository.MoodBookRepositoryImpl
import org.bookie.app.domain.usecase.SaveLibraryState
import org.bookie.app.domain.usecase.SearchBooksByMood
import org.bookie.app.presentation.search.MoodSearchViewModel

/**
 * Provides standalone public discovery when platform entry points still call App().
 * A future authenticated app composition root may supply AppFeatureViewModels.moodSearch
 * with the same JWT-aware Bookie client used by other features.
 */
@Composable
internal fun rememberConfiguredMoodSearchViewModel(): MoodSearchViewModel? {
    val baseUrl = platformBookieApiBaseUrl()?.trim().orEmpty()
    if (baseUrl.isBlank()) return null

    val client = remember(baseUrl) {
        runCatching { createPlatformHttpClient(BookieHttpClientConfig(baseUrl = baseUrl)) }
            .getOrNull()
    } ?: return null

    DisposableEffect(client) {
        onDispose { client.close() }
    }

    return remember(client) {
        MoodSearchViewModel(
            searchBooksByMood = SearchBooksByMood(
                MoodBookRepositoryImpl(KtorMoodBookRemoteDataSource(client)),
            ),
            saveLibraryState = SaveLibraryState(
                LibraryRepositoryImpl(KtorLibraryRemoteDataSource(client)),
            ),
        )
    }
}

/** Config is read from platform resources; no host address in MainActivity. */
@Composable
internal expect fun platformBookieApiBaseUrl(): String?
