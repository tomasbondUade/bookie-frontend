package org.bookie.app.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.bookie.app.domain.model.AuthState

interface AuthRepository {
    val state: StateFlow<AuthState>
    suspend fun restoreSession(): AuthState
    suspend fun signIn(email: String, password: String): AuthState
    suspend fun signUp(email: String, password: String, username: String): AuthState
    suspend fun signOut(): AuthState
}
