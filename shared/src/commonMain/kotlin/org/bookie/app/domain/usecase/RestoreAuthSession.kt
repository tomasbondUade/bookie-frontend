package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.AuthState
import org.bookie.app.domain.repository.AuthRepository

class RestoreAuthSession(private val repository: AuthRepository) {
    suspend operator fun invoke(): AuthState = repository.restoreSession()
}
