package org.bookie.app.presentation.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.bookie.app.domain.model.AuthState
import org.bookie.app.domain.repository.AuthRepository
import org.bookie.app.domain.repository.LibraryCacheSession
import org.bookie.app.domain.usecase.ClearLibraryCacheOnLogout
import org.bookie.app.domain.usecase.SignOutAndClearCache
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class AuthSignOutViewModelTest {
    @BeforeTest fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @AfterTest fun teardown() { Dispatchers.resetMain() }

    @Test fun `sign out switches protected state to form after cache cleanup`() = runTest {
        val repo = FakeAuth()
        var cleared: String? = null
        val clear = ClearLibraryCacheOnLogout(object : LibraryCacheSession {
            override suspend fun clearAccountCache(accountId: String) { cleared = accountId }
        })
        val vm = AuthViewModel(repo, SignOutAndClearCache(repo, clear))
        vm.restore()
        runCurrent()
        assertIs<AuthUiState.SignedIn>(vm.uiState.value)

        vm.signOut()
        assertEquals(AuthUiState.SigningOut, vm.uiState.value)
        runCurrent()
        assertEquals("user-1", cleared)
        assertIs<AuthUiState.Form>(vm.uiState.value)
        assertEquals(1, repo.signOutCount)
    }

    @Test fun `cache failure keeps session visible for retry`() = runTest {
        val repo = FakeAuth()
        val clear = ClearLibraryCacheOnLogout(object : LibraryCacheSession {
            override suspend fun clearAccountCache(accountId: String) { error("disk failure") }
        })
        val vm = AuthViewModel(repo, SignOutAndClearCache(repo, clear))
        vm.restore()
        runCurrent()
        vm.signOut()
        runCurrent()
        val state = assertIs<AuthUiState.SignedIn>(vm.uiState.value)
        assertEquals("user-1", state.userId)
        assertEquals(0, repo.signOutCount)
    }

    private class FakeAuth : AuthRepository {
        override val state = MutableStateFlow<AuthState>(AuthState.SignedOut)
        var signOutCount = 0
        override suspend fun restoreSession(): AuthState {
            state.value = AuthState.SignedIn("user-1", "reader@example.test", username = "reader")
            return state.value
        }
        override suspend fun signIn(email: String, password: String): AuthState = state.value
        override suspend fun signUp(email: String, password: String, username: String): AuthState = state.value
        override suspend fun signOut(): AuthState {
            signOutCount++
            state.value = AuthState.SignedOut
            return state.value
        }
    }
}
