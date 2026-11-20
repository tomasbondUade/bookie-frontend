package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.AuthState
import org.bookie.app.domain.repository.AuthRepository

/** Clear the prior account's cache BEFORE revoking the session. */
class SignOutAndClearCache(
    private val auth: AuthRepository,
    private val clearCache: ClearLibraryCacheOnLogout? = null,
) {
    suspend operator fun invoke(accountId: String?): AuthState {
        if (!accountId.isNullOrBlank()) clearCache?.invoke(accountId)
        return auth.signOut()
    }
}
