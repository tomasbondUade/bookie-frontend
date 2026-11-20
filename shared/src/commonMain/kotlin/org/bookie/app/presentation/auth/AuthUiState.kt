package org.bookie.app.presentation.auth

enum class AuthMode {
    SIGN_IN,
    SIGN_UP,
}

sealed interface AuthUiState {
    data object Restoring : AuthUiState

    data class Form(
        val mode: AuthMode = AuthMode.SIGN_IN,
        val email: String = "",
        val username: String = "",
        val error: String? = null,
    ) : AuthUiState

    data class Submitting(val mode: AuthMode) : AuthUiState

    data class SignedIn(val email: String?) : AuthUiState

    data class EmailVerificationRequired(val email: String) : AuthUiState
}
