package org.bookie.app

import org.bookie.app.data.auth.AuthClientBundle
import org.bookie.app.data.local.LibraryCache
import org.bookie.app.data.remote.KtorLibraryRemoteDataSource
import org.bookie.app.data.repository.AccountIdProvider
import org.bookie.app.data.repository.OnlineFirstLibraryRepository
import org.bookie.app.domain.model.AuthState
import org.bookie.app.domain.usecase.DeleteLibraryState
import org.bookie.app.domain.usecase.ObserveLibraryOnlineFirst
import org.bookie.app.domain.usecase.SaveLibraryState
import org.bookie.app.presentation.library.LibraryExperienceViewModel

/** Reuses the authenticated API client and account-scoped cache supplied by the platform. */
fun createAuthenticatedLibraryExperienceViewModel(
    authClients: AuthClientBundle,
    cache: LibraryCache,
): LibraryExperienceViewModel {
    val repository = OnlineFirstLibraryRepository(
        remote = KtorLibraryRemoteDataSource(authClients.bookieClient),
        cache = cache,
        accounts = AccountIdProvider {
            (authClients.repository.state.value as? AuthState.SignedIn)?.userId
        },
    )
    return LibraryExperienceViewModel(
        observeLibrary = ObserveLibraryOnlineFirst(repository),
        saveState = SaveLibraryState(repository),
        deleteState = DeleteLibraryState(repository),
    )
}
