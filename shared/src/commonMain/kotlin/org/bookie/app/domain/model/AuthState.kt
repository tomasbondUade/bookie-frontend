package org.bookie.app.domain.model

/** No access tokens or credentials are exposed to presentation. */
sealed interface AuthState {
    data object SignedOut : AuthState
    data object Loading : AuthState
    data class SignedIn(val userId: String, val email: String?) : AuthState
    data class EmailVerificationRequired(val email: String) : AuthState
    data object Expired : AuthState
    data class Error(val message: String) : AuthState
}
