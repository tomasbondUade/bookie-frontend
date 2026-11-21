package org.bookie.app

import org.bookie.app.data.auth.AuthClientBundle
import org.bookie.app.data.local.LibraryCache
import org.bookie.app.data.remote.KtorLibraryRemoteDataSource
import org.bookie.app.data.repository.AccountIdProvider
import org.bookie.app.data.repository.OnlineFirstLibraryRepository
import org.bookie.app.domain.model.AuthState
import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.usecase.CreateReadingSession
import org.bookie.app.domain.usecase.GetReadingSessions
import org.bookie.app.domain.usecase.DeleteLibraryState
import org.bookie.app.domain.usecase.ObserveLibraryOnlineFirst
import org.bookie.app.domain.usecase.SaveLibraryState
import org.bookie.app.presentation.library.LibraryExperienceViewModel
import org.bookie.app.presentation.progress.ReadingProgressViewModel

/**
 * The platform composition root supplies its existing JWT-aware API client,
 * persistent SQLDelight cache and authentication. No fake accounts or API URLs.
 *
 * Clear the old user's cache on logout/account switch (#63); this function
 * does not own the AuthClientBundle or its lifecycle.
 */
fun createAuthenticatedLibraryExperienceViewModel(
    authClients: AuthClientBundle,
    cache: LibraryCache,
): LibraryExperienceViewModel {
    val repository = authenticatedLibraryRepository(authClients, cache)
    return LibraryExperienceViewModel(
        observeLibrary = ObserveLibraryOnlineFirst(repository),
        saveState = SaveLibraryState(repository),
        deleteState = DeleteLibraryState(repository),
    )
}

fun createAuthenticatedReadingProgressViewModel(
    authClients: AuthClientBundle,
    cache: LibraryCache,
    entry: LibraryBookState,
): ReadingProgressViewModel {
    val repository = authenticatedLibraryRepository(authClients, cache)
    return ReadingProgressViewModel(
        libraryState = entry,
        saveLibraryState = SaveLibraryState(repository),
        createReadingSession = CreateReadingSession(repository),
        getReadingSessions = GetReadingSessions(repository),
    )
}

private fun authenticatedLibraryRepository(
    authClients: AuthClientBundle,
    cache: LibraryCache,
): OnlineFirstLibraryRepository = OnlineFirstLibraryRepository(
        remote = KtorLibraryRemoteDataSource(authClients.bookieClient),
        cache = cache,
        accounts = AccountIdProvider {
            (authClients.repository.state.value as? AuthState.SignedIn)?.userId
        },
    )
