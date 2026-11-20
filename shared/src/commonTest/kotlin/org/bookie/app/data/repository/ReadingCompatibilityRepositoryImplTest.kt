package org.bookie.app.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.bookie.app.data.remote.BookieHttpClientConfig
import org.bookie.app.data.remote.KtorReadingCompatibilityRemoteDataSource
import org.bookie.app.data.remote.ReadingCompatibilityRemoteDataSource
import org.bookie.app.data.remote.createBookieHttpClient
import org.bookie.app.data.remote.dto.CompatibilityUserDto
import org.bookie.app.data.remote.dto.ReadingCompatibilityDto
import org.bookie.app.domain.model.CompatibilityUser
import org.bookie.app.domain.model.ReadingCompatibility
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingCompatibilityRepositoryImplTest {
    @Test
    fun `maps the response into a domain compatibility result`() = runTest {
        val repository = ReadingCompatibilityRepositoryImpl(
            FakeRemoteDataSource(
                ReadingCompatibilityDto(
                    user = CompatibilityUserDto("user-2", "reader"),
                    compatibilityPercentage = 60,
                ),
            ),
        )

        assertEquals(
            Resource.Success(
                ReadingCompatibility(
                    user = CompatibilityUser("user-2", "reader", null),
                    compatibilityPercentage = 60,
                ),
            ),
            repository.getCompatibility("user-2"),
        )
    }

    @Test
    fun `marks unauthorized responses so presentation can request sign in`() = runTest {
        val client = createBookieHttpClient(
            engine = MockEngine { respond("", HttpStatusCode.Unauthorized) },
            config = BookieHttpClientConfig(baseUrl = "https://api.example.test/v1"),
        )
        val repository = ReadingCompatibilityRepositoryImpl(KtorReadingCompatibilityRemoteDataSource(client))

        assertEquals(
            Resource.Error(
                message = "El servidor respondió con HTTP 401.",
                kind = ResourceErrorKind.UNAUTHORIZED,
            ),
            repository.getCompatibility("user-2"),
        )
        client.close()
    }

    private class FakeRemoteDataSource(
        private val result: ReadingCompatibilityDto,
    ) : ReadingCompatibilityRemoteDataSource {
        override suspend fun getCompatibility(targetUserId: String) = result
    }
}
