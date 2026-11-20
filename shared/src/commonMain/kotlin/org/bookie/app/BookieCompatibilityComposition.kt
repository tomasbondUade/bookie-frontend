package org.bookie.app

import org.bookie.app.data.auth.AuthClientBundle
import org.bookie.app.data.remote.KtorReadingCompatibilityRemoteDataSource
import org.bookie.app.data.repository.ReadingCompatibilityRepositoryImpl
import org.bookie.app.domain.usecase.GetReadingCompatibility
import org.bookie.app.presentation.community.ReadingCompatibilityViewModel

/** Uses the shared authenticated Bookie client; the user identity always comes from its JWT. */
fun createAuthenticatedReadingCompatibilityViewModel(
    bundle: AuthClientBundle,
): ReadingCompatibilityViewModel = ReadingCompatibilityViewModel(
    GetReadingCompatibility(
        ReadingCompatibilityRepositoryImpl(
            KtorReadingCompatibilityRemoteDataSource(bundle.bookieClient),
        ),
    ),
)
