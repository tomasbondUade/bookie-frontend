package org.bookie.app.domain.repository

/**
 * Called by the authentication coordinator before ending or switching accounts.
 * The account ID must be captured while the old session is still available.
 */
interface LibraryCacheSession {
    suspend fun clearAccountCache(accountId: String)
}
