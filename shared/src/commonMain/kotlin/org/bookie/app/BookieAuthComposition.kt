package org.bookie.app

import org.bookie.app.data.auth.AuthClientBundle
import org.bookie.app.presentation.auth.AuthViewModel

fun createAuthViewModel(authClients: AuthClientBundle): AuthViewModel =
    AuthViewModel(authClients.repository)
