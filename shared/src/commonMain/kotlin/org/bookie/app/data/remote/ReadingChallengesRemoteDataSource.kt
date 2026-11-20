package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.bookie.app.data.remote.dto.JoinReadingChallengeDto
import org.bookie.app.data.remote.dto.ReadingChallengeDto
import org.bookie.app.data.remote.dto.ReadingChallengeProgressDto

interface ReadingChallengesRemoteDataSource {
    suspend fun getChallenges(): List<ReadingChallengeDto>
    suspend fun getMyProgress(): List<ReadingChallengeProgressDto>
    suspend fun joinChallenge(challengeId: String)
}

class KtorReadingChallengesRemoteDataSource(
    private val httpClient: HttpClient,
) : ReadingChallengesRemoteDataSource {
    override suspend fun getChallenges(): List<ReadingChallengeDto> =
        httpClient.get("challenges").body()

    override suspend fun getMyProgress(): List<ReadingChallengeProgressDto> =
        httpClient.get("challenges/my-progress").body()

    override suspend fun joinChallenge(challengeId: String) {
        require(challengeId.isNotBlank()) { "El ID del reto es obligatorio." }
        httpClient.post("challenges/join") {
            contentType(ContentType.Application.Json)
            setBody(JoinReadingChallengeDto(challengeId))
        }
    }
}
