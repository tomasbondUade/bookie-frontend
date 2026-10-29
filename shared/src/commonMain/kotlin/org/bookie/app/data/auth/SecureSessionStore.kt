package org.bookie.app.data.auth

import kotlinx.serialization.Serializable

/** Persisted only in Android Keystore-encrypted storage or iOS Keychain. */
@Serializable
data class StoredAuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
    val userId: String,
    val email: String?,
    val username: String,
    val syncPending: Boolean = true,
)

interface SecureSessionStore {
    suspend fun read(): StoredAuthSession?
    suspend fun write(session: StoredAuthSession)
    suspend fun clear()
}

/** Platform clock, used to evaluate expiration across process restarts. */
expect fun currentEpochSeconds(): Long
