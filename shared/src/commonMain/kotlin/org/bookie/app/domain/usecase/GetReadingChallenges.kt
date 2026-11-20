package org.bookie.app.domain.usecase

import org.bookie.app.domain.repository.ReadingChallengesRepository

class GetReadingChallenges(private val repository: ReadingChallengesRepository) {
    suspend operator fun invoke() = repository.getChallenges()
}

class GetMyReadingChallengeProgress(private val repository: ReadingChallengesRepository) {
    suspend operator fun invoke() = repository.getMyProgress()
}

class JoinReadingChallenge(private val repository: ReadingChallengesRepository) {
    suspend operator fun invoke(challengeId: String) = repository.joinChallenge(challengeId)
}
