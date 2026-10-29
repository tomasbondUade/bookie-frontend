package org.bookie.app.domain.usecase

import org.bookie.app.domain.repository.LibraryCacheSession

/** Call before logout or account switch, with the account ID of the old session. */
class ClearLibraryCacheOnLogout(private val cacheSession: LibraryCacheSession) {
    suspend operator fun invoke(accountId: String) {
        require(accountId.isNotBlank()) { "Account ID is required." }
        cacheSession.clearAccountCache(accountId)
    }
}
