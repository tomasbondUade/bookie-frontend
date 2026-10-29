package org.bookie.app.data.auth

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.bookie.app.domain.model.AuthState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SupabaseAuthRepositoryTest {
    private val now = 1_800_000_000L
    private val user = SupabaseUserDto("user-123", "reader@example.test", UserMetadataDto("reader"))
    private fun response(token: String = "jwt-1", refresh: String = "refresh-1") =
        SupabaseSessionDto(token, refresh, 3600L, user)
    private fun stored(expiresAt: Long = now + 3600L, pending: Boolean = false) =
        StoredAuthSession("jwt-old", "refresh-old", expiresAt, "user-123", "reader@example.test", "reader", pending)

    @Test fun `sign in persists session and synchronizes with Bookie JWT`() = runTest {
        val remote = FakeRemote().apply { loginResponse = response() }
        val store = MemoryStore()
        val sync = FakeSync()
        val repository = repo(remote, store, sync)

        val state = assertIs<AuthState.SignedIn>(repository.signIn(" reader@example.test ", "pass123"))
        assertEquals("user-123", state.userId)
        assertEquals("reader@example.test", remote.loginEmail)
        assertEquals(listOf("reader" to "jwt-1"), sync.calls)
        assertEquals("jwt-1", repository.getAccessToken())
        assertEquals(false, assertNotNull(store.value).syncPending)
        assertTrue(repository.state.value.toString().contains("SignedIn"))
        assertTrue(!repository.state.value.toString().contains("jwt-1"))
    }

    @Test fun `sign up with email verification does not invent a session or sync`() = runTest {
        val remote = FakeRemote().apply { signupResponse = SupabaseSessionDto(user = user) }
        val store = MemoryStore()
        val sync = FakeSync()
        val state = repo(remote, store, sync).signUp("reader@example.test", "pass123", "reader")
        assertIs<AuthState.EmailVerificationRequired>(state)
        assertNull(store.value)
        assertTrue(sync.calls.isEmpty())
    }

    @Test fun `sign up with immediate session synchronizes registered username`() = runTest {
        val remote = FakeRemote().apply { signupResponse = response() }
        val sync = FakeSync()
        val store = MemoryStore()
        val state = repo(remote, store, sync).signUp("reader@example.test", "pass123", "chosenName")
        assertIs<AuthState.SignedIn>(state)
        assertEquals("chosenName", remote.signupUsername)
        assertEquals(listOf("chosenName" to "jwt-1"), sync.calls)
    }

    @Test fun `restores valid session without refresh and retries pending sync`() = runTest {
        val remote = FakeRemote()
        val sync = FakeSync()
        val store = MemoryStore(stored(pending = true))
        val repository = repo(remote, store, sync)
        assertIs<AuthState.SignedIn>(repository.restoreSession())
        assertEquals(0, remote.refreshCalls)
        assertEquals(listOf("reader" to "jwt-old"), sync.calls)
        assertEquals("jwt-old", repository.getAccessToken())
    }

    @Test fun `restores expired session and rotates refresh token once`() = runTest {
        val remote = FakeRemote().apply { refreshResponse = response("jwt-new", "refresh-new") }
        val sync = FakeSync()
        val store = MemoryStore(stored(expiresAt = now - 10, pending = true))
        val repository = repo(remote, store, sync)
        assertIs<AuthState.SignedIn>(repository.restoreSession())
        assertEquals("refresh-old", remote.lastRefreshToken)
        assertEquals("jwt-new", repository.getAccessToken())
        assertEquals("refresh-new", store.value?.refreshToken)
        assertEquals(listOf("reader" to "jwt-new"), sync.calls)
    }

    @Test fun `concurrent access token requests refresh only once`() = runTest {
        val remote = FakeRemote().apply { refreshResponse = response("jwt-new", "refresh-new") }
        val repository = repo(remote, MemoryStore(stored(expiresAt = now - 10)), FakeSync())
        val tokens = (1..12).map { async { repository.getAccessToken() } }.awaitAll()
        assertEquals(List(12) { "jwt-new" }, tokens)
        assertEquals(1, remote.refreshCalls)
    }

    @Test fun `invalid refresh token expires and removes stored credentials`() = runTest {
        val remote = FakeRemote().apply { refreshError = UnauthorizedException() }
        val store = MemoryStore(stored(expiresAt = now - 10))
        val repository = repo(remote, store, FakeSync())
        assertNull(repository.getAccessToken())
        assertIs<AuthState.Expired>(repository.state.value)
        assertNull(store.value)
    }

    @Test fun `network outage on refresh preserves refresh token for retry`() = runTest {
        val remote = FakeRemote().apply { refreshError = kotlinx.io.IOException("offline") }
        val store = MemoryStore(stored(expiresAt = now - 10))
        val repository = repo(remote, store, FakeSync())
        assertNull(repository.getAccessToken())
        assertIs<AuthState.Error>(repository.state.value)
        assertEquals("refresh-old", store.value?.refreshToken)
        remote.refreshError = null
        remote.refreshResponse = response("jwt-new", "refresh-new")
        assertEquals("jwt-new", repository.getAccessToken())
    }

    @Test fun `sync outage keeps Supabase session and can retry on restoration`() = runTest {
        val remote = FakeRemote().apply { loginResponse = response() }
        val store = MemoryStore()
        val sync = FakeSync().apply { fail = true }
        val repository = repo(remote, store, sync)
        val state = assertIs<AuthState.SignedIn>(repository.signIn("reader@example.test", "pass123"))
        assertNotNull(state.syncError)
        assertEquals("jwt-1", repository.getAccessToken())
        assertEquals(true, store.value?.syncPending)
        sync.fail = false
        assertIs<AuthState.SignedIn>(repository.restoreSession())
        assertEquals(false, store.value?.syncPending)
    }

    @Test fun `sign out clears local tokens even when Supabase is offline`() = runTest {
        val remote = FakeRemote().apply { logoutError = kotlinx.io.IOException("offline") }
        val store = MemoryStore(stored())
        val repository = repo(remote, store, FakeSync())
        repository.restoreSession()
        assertIs<AuthState.SignedOut>(repository.signOut())
        assertNull(store.value)
        assertNull(repository.getAccessToken())
        assertEquals(1, remote.logoutCalls)
    }

    @Test fun `rejects invalid signup fields without making requests`() = runTest {
        val remote = FakeRemote()
        val repository = repo(remote, MemoryStore(), FakeSync())
        assertIs<AuthState.Error>(repository.signUp("bad", "123", "x"))
        assertEquals(0, remote.signupCalls)
    }

    @Test fun `restoring missing session reports signed out`() = runTest {
        assertIs<AuthState.SignedOut>(repo(FakeRemote(), MemoryStore(), FakeSync()).restoreSession())
    }

    private fun repo(remote: FakeRemote, store: MemoryStore, sync: FakeSync) =
        SupabaseAuthRepository(remote, store, sync, clock = { now })

    private class MemoryStore(var value: StoredAuthSession? = null) : SecureSessionStore {
        override suspend fun read(): StoredAuthSession? = value
        override suspend fun write(session: StoredAuthSession) { value = session }
        override suspend fun clear() { value = null }
    }

    private class FakeSync : BookieUserSyncRemoteDataSource {
        val calls = mutableListOf<Pair<String, String>>()
        var fail = false
        override suspend fun sync(username: String, accessToken: String) {
            calls += username to accessToken
            if (fail) throw kotlinx.io.IOException("offline")
        }
    }

    private class FakeRemote : SupabaseAuthRemoteDataSource {
        var loginResponse: SupabaseSessionDto = SupabaseSessionDto()
        var signupResponse: SupabaseSessionDto = SupabaseSessionDto()
        var refreshResponse: SupabaseSessionDto = SupabaseSessionDto()
        var refreshError: Exception? = null
        var logoutError: Exception? = null
        var loginEmail: String? = null
        var signupUsername: String? = null
        var signupCalls = 0
        var refreshCalls = 0
        var logoutCalls = 0
        var lastRefreshToken: String? = null
        override suspend fun signIn(email: String, password: String): SupabaseSessionDto {
            loginEmail = email
            return loginResponse
        }
        override suspend fun signUp(email: String, password: String, username: String): SupabaseSessionDto {
            signupCalls++
            signupUsername = username
            return signupResponse
        }
        override suspend fun refresh(refreshToken: String): SupabaseSessionDto {
            refreshCalls++
            lastRefreshToken = refreshToken
            refreshError?.let { throw it }
            return refreshResponse
        }
        override suspend fun signOut(accessToken: String) {
            logoutCalls++
            logoutError?.let { throw it }
        }
    }

    /** Real HTTP exception to exercise ApiError.Http(401) mapping. */
    private class UnauthorizedException : Exception() {
        override val message: String = "invalid refresh token"
    }
}
