package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.bookie.app.data.remote.dto.*

interface TinderRemoteDataSource {
    suspend fun getFeed(limit: Int): TinderFeedDto
    suspend fun sendSwipe(input: CreateSwipeDto): SwipeResponseDto
}

class KtorTinderRemoteDataSource(private val client: HttpClient) : TinderRemoteDataSource {
    override suspend fun getFeed(limit: Int): TinderFeedDto =
        client.get("books/tinder") { parameter("limit", limit) }.body()

    override suspend fun sendSwipe(input: CreateSwipeDto): SwipeResponseDto =
        client.post("swipes") {
            contentType(ContentType.Application.Json)
            setBody(input)
        }.body()
}
