package org.bookie.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bookie.app.domain.model.AuthState
import org.bookie.app.domain.repository.AuthRepository
import org.bookie.app.domain.usecase.RestoreAuthSession
import org.bookie.app.domain.usecase.SignIn
import org.bookie.app.domain.usecase.SignUp

class AuthViewModel(repository: AuthRepository) : ViewModel() {
    private val restoreSession = RestoreAuthSession(repository)
    private val signIn = SignIn(repository)
    private val signUp = SignUp(repository)
    private val mutableUiState = MutableStateFlow<AuthUiState>(AuthUiState.Restoring)
    val uiState = mutableUiState.asStateFlow()

    fun restore() {
        if (mutableUiState.value is AuthUiState.Submitting) return
        mutableUiState.value = AuthUiState.Restoring
        viewModelScope.launch {
            mutableUiState.value = restoreSession().toUiState()
        }
    }

    fun chooseMode(mode: AuthMode) {
        val current = mutableUiState.value as? AuthUiState.Form ?: AuthUiState.Form()
        mutableUiState.value = current.copy(mode = mode, error = null)
    }

    fun updateEmail(email: String) = updateForm { copy(email = email, error = null) }

    fun updateUsername(username: String) = updateForm { copy(username = username, error = null) }

    fun submit(password: String) {
        val form = mutableUiState.value as? AuthUiState.Form ?: return
        if (form.mode == AuthMode.SIGN_IN && password.isBlank()) {
            mutableUiState.value = form.copy(error = "Ingresá tu contraseña.")
            return
        }
        if (form.mode == AuthMode.SIGN_UP) {
            when {
                !isValidEmail(form.email) -> {
                    mutableUiState.value = form.copy(error = "Ingresá un correo válido.")
                    return
                }
                password.length < 6 -> {
                    mutableUiState.value = form.copy(error = "La contraseña debe tener al menos 6 caracteres.")
                    return
                }
                form.username.trim().length !in 3..30 -> {
                    mutableUiState.value = form.copy(error = "El usuario debe tener entre 3 y 30 caracteres.")
                    return
                }
            }
        } else if (!isValidEmail(form.email)) {
            mutableUiState.value = form.copy(error = "Ingresá un correo válido.")
            return
        }

        mutableUiState.value = AuthUiState.Submitting(form.mode)
        viewModelScope.launch {
            val nextState = try {
                when (form.mode) {
                    AuthMode.SIGN_IN -> signIn(form.email, password)
                    AuthMode.SIGN_UP -> signUp(form.email, password, form.username)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                AuthState.Error(exception.message ?: "No se pudo completar la solicitud.")
            }
            mutableUiState.value = nextState.toUiState(form)
        }
    }

    private fun updateForm(update: AuthUiState.Form.() -> AuthUiState.Form) {
        val current = mutableUiState.value as? AuthUiState.Form ?: return
        mutableUiState.value = current.update()
    }

    private fun AuthState.toUiState(previous: AuthUiState.Form = AuthUiState.Form()): AuthUiState =
        when (this) {
            is AuthState.SignedIn -> AuthUiState.SignedIn(email)
            is AuthState.EmailVerificationRequired -> AuthUiState.EmailVerificationRequired(email)
            is AuthState.Error -> previous.copy(error = message)
            AuthState.Expired -> previous.copy(error = "Tu sesión venció. Iniciá sesión nuevamente.")
            AuthState.SignedOut -> previous
            AuthState.Loading -> AuthUiState.Submitting(previous.mode)
        }

    private fun isValidEmail(value: String): Boolean {
        val email = value.trim()
        return email.contains('@') && email.substringAfter('@').contains('.') && !email.contains(' ')
    }
}
