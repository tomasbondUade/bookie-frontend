package org.bookie.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bookie.app.domain.model.AuthState
import org.bookie.app.domain.repository.AuthRepository
import org.bookie.app.domain.usecase.RestoreAuthSession
import org.bookie.app.domain.usecase.SignIn
import org.bookie.app.domain.usecase.SignUp

class AuthViewModel(
    private val repository: AuthRepository,
    private val clearAccountCache: suspend (String) -> Unit = {},
) : ViewModel() {
    private val restoreSession = RestoreAuthSession(repository)
    private val signIn = SignIn(repository)
    private val signUp = SignUp(repository)
    private val mutableUiState = MutableStateFlow<AuthUiState>(AuthUiState.Restoring)
    val uiState = mutableUiState.asStateFlow()

    private var restoring = false
    private var restoreRequested = false

    init {
        // Expiry can occur while a protected screen is active.
        viewModelScope.launch {
            repository.state.collect { state ->
                when (state) {
                    AuthState.Expired -> mutableUiState.value = AuthUiState.Form(
                        error = "Tu sesión venció. Iniciá sesión nuevamente.",
                    )
                    AuthState.SignedOut -> if (mutableUiState.value is AuthUiState.SignedIn) {
                        mutableUiState.value = AuthUiState.Form()
                    }
                    is AuthState.SignedIn -> if (mutableUiState.value is AuthUiState.SignedIn) {
                        mutableUiState.value = state.toUiState()
                    }
                    is AuthState.Error -> if (mutableUiState.value is AuthUiState.SignedIn) {
                        mutableUiState.value = AuthUiState.Form(error = state.message)
                    }
                    AuthState.Loading, is AuthState.EmailVerificationRequired -> Unit
                }
            }
        }
    }

    fun restore() {
        if (restoreRequested || restoring || mutableUiState.value is AuthUiState.Submitting ||
            mutableUiState.value is AuthUiState.SignedIn || mutableUiState.value == AuthUiState.SigningOut) return
        restoreRequested = true
        restoring = true
        mutableUiState.value = AuthUiState.Restoring
        viewModelScope.launch {
            try {
                mutableUiState.value = restoreSession().toUiState()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                mutableUiState.value = AuthUiState.Form(error = "No se pudo restaurar la sesión.")
            } finally {
                restoring = false
            }
        }
    }

    fun chooseMode(mode: AuthMode) {
        if (restoring || mutableUiState.value is AuthUiState.Submitting || mutableUiState.value is AuthUiState.SignedIn || mutableUiState.value == AuthUiState.SigningOut) return
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

    /** Reattempt a pending user sync without invalidating the Supabase session. */
    fun retrySync() {
        val signedIn = mutableUiState.value as? AuthUiState.SignedIn ?: return
        if (signedIn.syncWarning == null) return
        mutableUiState.value = signedIn.copy(syncWarning = "Reintentando sincronización con Bookie…")
        viewModelScope.launch {
            try {
                mutableUiState.value = restoreSession().toUiState()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                mutableUiState.value = signedIn.copy(
                    syncWarning = "No se pudo reintentar la sincronización con Bookie.",
                )
            }
        }
    }

    fun signOut() {
        val signedIn = mutableUiState.value as? AuthUiState.SignedIn ?: return
        val userId = signedIn.userId?.takeIf(String::isNotBlank) ?: return
        mutableUiState.value = AuthUiState.SigningOut
        viewModelScope.launch {
            // Do not invalidate the session until this account's cache is cleared.
            try {
                clearAccountCache(userId)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                mutableUiState.value = signedIn.copy(
                    signOutError = "No se pudo limpiar la biblioteca local. Reintentá cerrar sesión.",
                )
                return@launch
            }
            mutableUiState.value = try {
                repository.signOut().toUiState()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                AuthUiState.Form(error = "No se pudo completar el cierre de sesión.")
            }
        }
    }

    private fun updateForm(update: AuthUiState.Form.() -> AuthUiState.Form) {
        val current = mutableUiState.value as? AuthUiState.Form ?: return
        mutableUiState.value = current.update()
    }

    private fun AuthState.toUiState(previous: AuthUiState.Form = AuthUiState.Form()): AuthUiState =
        when (this) {
            is AuthState.SignedIn -> AuthUiState.SignedIn(email, userId, username, syncWarning = syncError)
            is AuthState.EmailVerificationRequired -> AuthUiState.EmailVerificationRequired(email)
            is AuthState.Error -> previous.copy(error = message)
            AuthState.Expired -> previous.copy(error = "Tu sesión venció. Iniciá sesión nuevamente.")
            AuthState.SignedOut -> AuthUiState.Form()
            AuthState.Loading -> AuthUiState.Submitting(previous.mode)
        }

    private fun isValidEmail(value: String): Boolean {
        val email = value.trim()
        return email.count { it == '@' } == 1 && email.substringBefore('@').isNotEmpty() &&
            email.substringAfter('@').split('.').let { parts -> parts.size >= 2 && parts.all { it.isNotEmpty() } } &&
            email.none { it.isWhitespace() }
    }
}
