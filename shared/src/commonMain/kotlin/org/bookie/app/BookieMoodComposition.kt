package org.bookie.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import io.ktor.client.HttpClient
import org.bookie.app.data.auth.AuthClientBundle
import org.bookie.app.domain.repository.AuthRepository
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
 * Public fallback cannot save books, because it has no authenticated session.
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
        composeMoodSearchViewModel(client = client, authRepository = null)
    }
}

/** Config is read from platform resources; no host address in MainActivity. */
@Composable
internal expect fun platformBookieApiBaseUrl(): String?

/**
 * Build an authenticated mood-search feature at the app composition root.
 *
 * Reuses #8's AuthClientBundle: bookieClient already injects the current Supabase
 * JWT and refreshes it through repository. Ownership/cleanup of the bundle stays
 * with the application root, just like other authenticated features.
 *
 * Supply this ViewModel through AppFeatureViewModels.moodSearch when the
 * app's shared session/login entry point (#27) is wired.
 */
fun createAuthenticatedMoodSearchViewModel(authClients: AuthClientBundle): MoodSearchViewModel =
    composeMoodSearchViewModel(
        client = authClients.bookieClient,
        authRepository = authClients.repository,
    )

private fun composeMoodSearchViewModel(
    client: HttpClient,
    authRepository: AuthRepository?,
): MoodSearchViewModel = MoodSearchViewModel(
    searchBooksByMood = SearchBooksByMood(
        MoodBookRepositoryImpl(KtorMoodBookRemoteDataSource(client)),
    ),
    saveLibraryState = SaveLibraryState(
        LibraryRepositoryImpl(KtorLibraryRemoteDataSource(client)),
    ),
    authRepository = authRepository,
)
