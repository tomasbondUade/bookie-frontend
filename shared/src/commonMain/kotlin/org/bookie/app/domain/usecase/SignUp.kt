package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.AuthState
import org.bookie.app.domain.repository.AuthRepository

class SignUp(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String, username: String): AuthState =
        repository.signUp(email, password, username)
}
