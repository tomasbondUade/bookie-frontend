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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSessionFlowTest {
    @BeforeTest fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @AfterTest fun teardown() { Dispatchers.resetMain() }

    @Test fun `logout clears cache before Supabase and returns to login`() = runTest {
        val calls = mutableListOf<String>()
        val repository = FakeAuth(calls)
        val vm = AuthViewModel(repository, clearAccountCache = { calls.add("cache:$it") })
        vm.restore()
        runCurrent()
        assertIs<AuthUiState.SignedIn>(vm.uiState.value)
        vm.signOut()
        assertEquals(AuthUiState.SigningOut, vm.uiState.value)
        runCurrent()
        assertEquals(listOf("cache:user-a", "supabase-sign-out"), calls)
        assertIs<AuthUiState.Form>(vm.uiState.value)
    }

    @Test fun `failed cache cleanup keeps the account signed in for retry`() = runTest {
        val calls = mutableListOf<String>()
        val vm = AuthViewModel(FakeAuth(calls), clearAccountCache = { error("disk failure") })
        vm.restore()
        runCurrent()
        vm.signOut()
        runCurrent()
        val signedIn = assertIs<AuthUiState.SignedIn>(vm.uiState.value)
        assertEquals("user-a", signedIn.userId)
        assertEquals(true, signedIn.signOutError?.contains("Reintentá") == true)
        assertEquals(emptyList(), calls)
    }

    @Test fun `account state includes only real username email and sync warning`() = runTest {
        val repo = FakeAuth(mutableListOf()).apply {
            restored = AuthState.SignedIn("user-a", "reader@example.test",
                syncError = "Sin conexión con Bookie", username = "lectora")
        }
        val vm = AuthViewModel(repo)
        vm.restore()
        runCurrent()
        val state = assertIs<AuthUiState.SignedIn>(vm.uiState.value)
        assertEquals("lectora", state.username)
        assertEquals("Sin conexión con Bookie", state.syncWarning)
    }

    private class FakeAuth(private val calls: MutableList<String>) : AuthRepository {
        override val state = MutableStateFlow<AuthState>(AuthState.SignedOut)
        var restored: AuthState = AuthState.SignedIn("user-a", "reader@example.test", username = "lectora")
        override suspend fun restoreSession(): AuthState = restored
        override suspend fun signIn(email: String, password: String): AuthState = restored
        override suspend fun signUp(email: String, password: String, username: String): AuthState = restored
        override suspend fun signOut(): AuthState {
            calls.add("supabase-sign-out")
            state.value = AuthState.SignedOut
            return AuthState.SignedOut
        }
    }
}
