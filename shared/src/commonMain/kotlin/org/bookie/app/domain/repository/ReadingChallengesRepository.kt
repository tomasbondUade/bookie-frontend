package org.bookie.app.domain.repository

import org.bookie.app.domain.model.ReadingChallenge
import org.bookie.app.domain.model.ReadingChallengeProgress
import org.bookie.app.domain.result.Resource

interface ReadingChallengesRepository {
    suspend fun getChallenges(): Resource<List<ReadingChallenge>>
    suspend fun getMyProgress(): Resource<List<ReadingChallengeProgress>>
    suspend fun joinChallenge(challengeId: String): Resource<Unit>
}
