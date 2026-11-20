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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `rejects invalid registration before calling the repository`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)
        viewModel.restore()
        runCurrent()
        viewModel.chooseMode(AuthMode.SIGN_UP)
        viewModel.updateEmail("invalid")
        viewModel.updateUsername("reader")

        viewModel.submit("secret123")

        assertEquals("Ingresá un correo válido.", (viewModel.uiState.value as AuthUiState.Form).error)
        assertEquals(0, repository.signUpCalls)
    }

    @Test
    fun `shows email verification instead of inventing an authenticated session`() = runTest {
        val repository = FakeAuthRepository().apply {
            signUpResult = AuthState.EmailVerificationRequired("reader@example.test")
        }
        val viewModel = AuthViewModel(repository)
        viewModel.restore()
        runCurrent()
        viewModel.chooseMode(AuthMode.SIGN_UP)
        viewModel.updateEmail("reader@example.test")
        viewModel.updateUsername("reader")

        viewModel.submit("secret123")
        runCurrent()

        assertEquals(
            AuthUiState.EmailVerificationRequired("reader@example.test"),
            viewModel.uiState.value,
        )
        assertEquals(1, repository.signUpCalls)
        assertTrue(!viewModel.uiState.value.toString().contains("secret123"))
    }

    @Test
    fun `signs in and exposes only the account email in presentation state`() = runTest {
        val repository = FakeAuthRepository().apply {
            signInResult = AuthState.SignedIn("account-id", "reader@example.test")
        }
        val viewModel = AuthViewModel(repository)
        viewModel.restore()
        runCurrent()
        viewModel.updateEmail("reader@example.test")

        viewModel.submit("secret123")
        runCurrent()

        assertEquals(AuthUiState.SignedIn("reader@example.test", "account-id"), viewModel.uiState.value)
        assertEquals(1, repository.signInCalls)
        assertTrue(!viewModel.uiState.value.toString().contains("secret123"))
    }

    private class FakeAuthRepository : AuthRepository {
        override val state = MutableStateFlow<AuthState>(AuthState.SignedOut)
        var restoreResult: AuthState = AuthState.SignedOut
        var signInResult: AuthState = AuthState.Error("Invalid credentials")
        var signUpResult: AuthState = AuthState.Error("Registration failed")
        var signInCalls = 0
        var signUpCalls = 0

        override suspend fun restoreSession(): AuthState = restoreResult
        override suspend fun signIn(email: String, password: String): AuthState {
            signInCalls++
            return signInResult
        }
        override suspend fun signUp(email: String, password: String, username: String): AuthState {
            signUpCalls++
            return signUpResult
        }

        override suspend fun signOut(): AuthState = AuthState.SignedOut
    }
}
