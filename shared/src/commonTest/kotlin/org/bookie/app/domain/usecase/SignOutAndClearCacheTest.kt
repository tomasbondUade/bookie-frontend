package org.bookie.app.domain.usecase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.bookie.app.domain.model.AuthState
import org.bookie.app.domain.repository.AuthRepository
import org.bookie.app.domain.repository.LibraryCacheSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SignOutAndClearCacheTest {
    @Test
    fun `cleans only the outgoing account before revoking the token`() = runTest {
        val events = mutableListOf<String>()
        val auth = StubAuth { events += "logout" }
        val cache = ClearLibraryCacheOnLogout(object : LibraryCacheSession {
            override suspend fun clearAccountCache(accountId: String) {
                events += "cache:$accountId"
            }
        })
        val result = SignOutAndClearCache(auth, cache)("user-a")
        assertEquals(AuthState.SignedOut, result)
        assertEquals(listOf("cache:user-a", "logout"), events)
    }

    @Test
    fun `failed cache cleanup prevents logout and can be retried`() = runTest {
        val events = mutableListOf<String>()
        val auth = StubAuth { events += "logout" }
        var fails = true
        val cache = ClearLibraryCacheOnLogout(object : LibraryCacheSession {
            override suspend fun clearAccountCache(accountId: String) {
                events += "clear:$accountId"
                if (fails) error("storage inaccessible")
            }
        })
        val logout = SignOutAndClearCache(auth, cache)
        assertFailsWith<IllegalStateException> { logout("user-a") }
        assertEquals(listOf("clear:user-a"), events)
        fails = false
        assertEquals(AuthState.SignedOut, logout("user-a"))
        assertEquals(listOf("clear:user-a", "clear:user-a", "logout"), events)
    }

    private class StubAuth(private val onSignOut: () -> Unit) : AuthRepository {
        override val state = MutableStateFlow<AuthState>(AuthState.SignedIn("user-a", "a@example.test"))
        override suspend fun restoreSession(): AuthState = state.value
        override suspend fun signIn(email: String, password: String): AuthState = state.value
        override suspend fun signUp(email: String, password: String, username: String): AuthState = state.value
        override suspend fun signOut(): AuthState {
            onSignOut()
            state.value = AuthState.SignedOut
            return state.value
        }
    }
}
