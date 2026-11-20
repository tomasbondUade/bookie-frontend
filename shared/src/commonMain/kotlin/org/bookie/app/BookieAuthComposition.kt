package org.bookie.app

import org.bookie.app.data.auth.AuthClientBundle
import org.bookie.app.data.local.LibraryCache
import org.bookie.app.presentation.auth.AuthViewModel

fun createAuthViewModel(
    authClients: AuthClientBundle,
    cache: LibraryCache,
): AuthViewModel = AuthViewModel(
    repository = authClients.repository,
    clearAccountCache = cache::clearAccount,
)
