package org.bookie.app.data.repository

/** Supplied by authentication; never derive the account from a book or a token string. */
fun interface AccountIdProvider {
    suspend fun currentAccountId(): String?
}
