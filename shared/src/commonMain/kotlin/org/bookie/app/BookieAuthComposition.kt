package org.bookie.app

import org.bookie.app.data.auth.AuthClientBundle
import org.bookie.app.presentation.auth.AuthViewModel
import org.bookie.app.data.local.LibraryCache
import org.bookie.app.domain.repository.LibraryCacheSession
import org.bookie.app.domain.usecase.ClearLibraryCacheOnLogout
import org.bookie.app.domain.usecase.SignOutAndClearCache

fun createAuthViewModel(authClients: AuthClientBundle, cache: LibraryCache? = null): AuthViewModel {
    val clear = cache?.let { libraryCache ->
        ClearLibraryCacheOnLogout(object : LibraryCacheSession {
            override suspend fun clearAccountCache(accountId: String) {
                libraryCache.clearAccount(accountId)
            }
        })
    }
    return AuthViewModel(
        repository = authClients.repository,
        signOutAndClearCache = SignOutAndClearCache(authClients.repository, clear),
    )
}
