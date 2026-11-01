package org.bookie.app.data.auth

import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bookie.app.data.remote.AccessTokenProvider
import org.bookie.app.data.remote.ApiError
import org.bookie.app.data.remote.toApiError
import org.bookie.app.data.remote.toMessage
import org.bookie.app.domain.model.AuthState
import org.bookie.app.domain.repository.AuthRepository

/**
 * One session coordinator shared by Bookie's HTTP token provider and the login UI.
 * Mutex serializes refresh-token rotation and prevents parallel refresh requests.
 */
class SupabaseAuthRepository(
    private val remote: SupabaseAuthRemoteDataSource,
    private val store: SecureSessionStore,
    private val userSync: BookieUserSyncRemoteDataSource,
    private val clock: () -> Long = ::currentEpochSeconds,
) : AuthRepository, AccessTokenProvider {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    override val state = mutableState.asStateFlow()
    private var session: StoredAuthSession? = null

    override suspend fun restoreSession(): AuthState = mutex.withLock {
        mutableState.value = AuthState.Loading
        try {
            val restored = store.read()
            session = restored
            if (restored == null) {
                mutableState.value = AuthState.SignedOut
            } else {
                val current = if (needsRefresh(restored)) refreshLocked(restored) else restored
                if (current != null) {
                    syncIfNeededLocked(current)
                }
            }
        } catch (error: Exception) {
            mutableState.value = AuthState.Error(error.toApiError().toMessage())
        }
        mutableState.value
    }

    override suspend fun signIn(email: String, password: String): AuthState = mutex.withLock {
        val cleanEmail = email.trim()
        if (!validEmail(cleanEmail) || password.isBlank()) {
            return@withLock AuthState.Error("Ingresá un correo y contraseña válidos.")
                .also { mutableState.value = it }
        }
        mutableState.value = AuthState.Loading
        try {
            store.clear()
            session = null
            val response = remote.signIn(cleanEmail, password)
            val stored = createSession(response, cleanEmail, null)
            store.write(stored)
            session = stored
            syncIfNeededLocked(stored)
        } catch (error: Exception) {
            mutableState.value = AuthState.Error(authErrorMessage(error))
        }
        mutableState.value
    }

    override suspend fun signUp(
        email: String,
        password: String,
        username: String,
    ): AuthState = mutex.withLock {
        val cleanEmail = email.trim()
        val cleanUsername = username.trim()
        if (!validEmail(cleanEmail) || password.length < 6 || cleanUsername.length !in 3..30) {
            return@withLock AuthState.Error(
                "Ingresá un correo válido, una contraseña de al menos 6 caracteres y un usuario de 3 a 30.",
            ).also { mutableState.value = it }
        }
        mutableState.value = AuthState.Loading
        try {
            store.clear()
            session = null
            val response = remote.signUp(cleanEmail, password, cleanUsername)
            if (response.accessToken.isNullOrBlank()) {
                // Email confirmation is enabled in Supabase: do not invent a session.
                mutableState.value = AuthState.EmailVerificationRequired(cleanEmail)
            } else {
                val stored = createSession(response, cleanEmail, cleanUsername)
                store.write(stored)
                session = stored
                syncIfNeededLocked(stored)
            }
        } catch (error: Exception) {
            mutableState.value = AuthState.Error(authErrorMessage(error))
        }
        mutableState.value
    }

    override suspend fun signOut(): AuthState = mutex.withLock {
        val token = session?.accessToken
        // A network outage must not prevent local sign-out.
        try {
            if (!token.isNullOrBlank()) remote.signOut(token)
        } catch (error: Exception) {
            error.toApiError() // rethrows coroutine cancellation
        }
        try {
            store.clear()
            session = null
            mutableState.value = AuthState.SignedOut
        } catch (error: Exception) {
            session = null
            mutableState.value = AuthState.Error("No se pudo borrar la sesión del dispositivo.")
        }
        mutableState.value
    }

    override suspend fun getAccessToken(): String? = mutex.withLock {
        val current = try {
            session ?: store.read()?.also { session = it }
        } catch (error: Exception) {
            error.toApiError()
            mutableState.value = AuthState.Error("No se pudo leer la sesión guardada.")
            return@withLock null
        } ?: return@withLock null

        val active = if (needsRefresh(current)) refreshLocked(current) else current
        if (active != null) {
            if (mutableState.value !is AuthState.SignedIn) {
                mutableState.value = AuthState.SignedIn(active.userId, active.email)
            }
            active.accessToken
        } else null
    }

    private fun needsRefresh(value: StoredAuthSession): Boolean =
        value.expiresAtEpochSeconds <= clock() + REFRESH_MARGIN_SECONDS

    private suspend fun refreshLocked(previous: StoredAuthSession): StoredAuthSession? {
        return try {
            val response = remote.refresh(previous.refreshToken)
            val refreshed = createSession(response, previous.email, previous.username, previous)
            if (refreshed.userId != previous.userId) {
                expireLocked()
                return null
            }
            store.write(refreshed)
            session = refreshed
            syncIfNeededLocked(refreshed)
            session
        } catch (error: Exception) {
            when (val mapped = error.toApiError()) {
                is ApiError.Http -> if (mapped.statusCode == 400 || mapped.statusCode == 401) {
                    expireLocked()
                } else mutableState.value = AuthState.Error(mapped.toMessage())
                else -> mutableState.value = AuthState.Error(mapped.toMessage())
            }
            null
        }
    }

    private suspend fun expireLocked() {
        session = null
        store.clear()
        mutableState.value = AuthState.Expired
    }

    /** Never discard a valid Supabase session merely because Bookie is offline. */
    private suspend fun syncIfNeededLocked(value: StoredAuthSession) {
        if (!value.syncPending) {
            mutableState.value = AuthState.SignedIn(value.userId, value.email)
            return
        }
        try {
            userSync.sync(value.username, value.accessToken)
            val synced = value.copy(syncPending = false)
            store.write(synced)
            session = synced
            mutableState.value = AuthState.SignedIn(value.userId, value.email)
        } catch (error: Exception) {
            val mapped = error.toApiError()
            mutableState.value = AuthState.SignedIn(
                value.userId,
                value.email,
                syncError = "La sesión está activa, pero no se pudo sincronizar con Bookie: ${mapped.toMessage()}",
            )
        }
    }

    private fun createSession(
        response: SupabaseSessionDto,
        email: String?,
        username: String?,
        previous: StoredAuthSession? = null,
    ): StoredAuthSession {
        val access = response.accessToken?.takeIf { it.isNotBlank() }
            ?: error("Supabase no devolvió un access token.")
        val refresh = response.refreshToken?.takeIf { it.isNotBlank() }
            ?: previous?.refreshToken
            ?: error("Supabase no devolvió un refresh token.")
        val userId = response.user?.id ?: previous?.userId
            ?: error("Supabase no devolvió un usuario.")
        val name = username ?: response.user?.metadata?.username ?: previous?.username
            ?: email?.substringBefore('@')?.filter { it.isLetterOrDigit() || it == '_' }
                ?.take(30)?.padEnd(3, '_')
            ?: error("No se pudo determinar el nombre de usuario.")
        val lifetime = response.expiresIn?.takeIf { it > 0 }
            ?: error("Supabase no devolvió una expiración válida.")
        return StoredAuthSession(
            accessToken = access,
            refreshToken = refresh,
            expiresAtEpochSeconds = clock() + lifetime,
            userId = userId,
            email = response.user?.email ?: email ?: previous?.email,
            username = name,
            syncPending = previous?.syncPending ?: true,
        )
    }

    private fun validEmail(value: String) =
        value.contains('@') && value.substringAfter('@').contains('.') && !value.contains(' ')

    private fun authErrorMessage(error: Exception): String = when (val mapped = error.toApiError()) {
        is ApiError.Http -> if (mapped.statusCode in listOf(400, 401, 422)) {
            "Credenciales inválidas o solicitud rechazada por Supabase."
        } else mapped.toMessage()
        else -> mapped.toMessage()
    }

    private companion object {
        const val REFRESH_MARGIN_SECONDS = 60L
    }
}
