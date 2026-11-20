package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.AuthState
import org.bookie.app.domain.repository.AuthRepository

class SignIn(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AuthState =
        repository.signIn(email, password)
}
