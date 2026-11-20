package org.bookie.app

import org.bookie.app.data.auth.AuthClientBundle
import org.bookie.app.data.remote.KtorReadingChallengesRemoteDataSource
import org.bookie.app.data.repository.ReadingChallengesRepositoryImpl
import org.bookie.app.domain.usecase.GetMyReadingChallengeProgress
import org.bookie.app.domain.usecase.GetReadingChallenges
import org.bookie.app.domain.usecase.JoinReadingChallenge
import org.bookie.app.presentation.challenges.ReadingChallengesViewModel

/** Uses the shared JWT-aware client; challenge identity is derived from the active session. */
fun createAuthenticatedReadingChallengesViewModel(
    authClients: AuthClientBundle,
): ReadingChallengesViewModel {
    val repository = ReadingChallengesRepositoryImpl(
        remote = KtorReadingChallengesRemoteDataSource(authClients.bookieClient),
        tokenProvider = authClients.repository,
    )
    return ReadingChallengesViewModel(
        getChallenges = GetReadingChallenges(repository),
        getMyProgress = GetMyReadingChallengeProgress(repository),
        joinChallenge = JoinReadingChallenge(repository),
    )
}
