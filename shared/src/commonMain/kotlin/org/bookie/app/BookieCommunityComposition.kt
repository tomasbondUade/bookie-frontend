package org.bookie.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import org.bookie.app.data.auth.AuthClientBundle
import org.bookie.app.data.remote.BookieHttpClientConfig
import org.bookie.app.data.remote.AccessTokenProvider
import org.bookie.app.data.remote.KtorCommunityPostsRemoteDataSource
import org.bookie.app.data.remote.KtorCommunityRemoteDataSource
import org.bookie.app.data.remote.createPlatformHttpClient
import org.bookie.app.data.repository.CommunityPostsRepositoryImpl
import org.bookie.app.data.repository.CommunityRepositoryImpl
import org.bookie.app.domain.repository.AuthRepository
import org.bookie.app.domain.usecase.CreateCommunity
import org.bookie.app.domain.usecase.GetCommunities
import org.bookie.app.domain.usecase.GetCommunityPosts
import org.bookie.app.domain.usecase.GetRecommendedCommunities
import org.bookie.app.presentation.community.CommunityViewModel
import io.ktor.client.HttpClient

@Composable
internal fun rememberConfiguredCommunityViewModel(): CommunityViewModel? {
    val baseUrl = platformBookieApiBaseUrl()?.trim().orEmpty()
    if (baseUrl.isBlank()) return null

    val client = remember(baseUrl) {
        runCatching { createPlatformHttpClient(BookieHttpClientConfig(baseUrl = baseUrl)) }
            .getOrNull()
    } ?: return null

    DisposableEffect(client) { onDispose { client.close() } }

    return remember(client) { makeCommunityViewModel(client, null, AccessTokenProvider { null }) }
}

/** Use the authenticated client's existing JWT provider from #8/#34.
 * Supply through AppFeatureViewModels.community alongside the shared auth ViewModel.
 */
fun createAuthenticatedCommunityViewModel(bundle: AuthClientBundle): CommunityViewModel =
    makeCommunityViewModel(bundle.bookieClient, bundle.repository, bundle.repository)

private fun makeCommunityViewModel(
    client: HttpClient,
    auth: AuthRepository?,
    tokenProvider: AccessTokenProvider,
): CommunityViewModel {
    val repository = CommunityRepositoryImpl(KtorCommunityRemoteDataSource(client))
    val postsRepository = CommunityPostsRepositoryImpl(
        KtorCommunityPostsRemoteDataSource(client), tokenProvider,
    )
    return CommunityViewModel(
        getCommunities = GetCommunities(repository),
        createCommunity = CreateCommunity(repository),
        getRecommended = GetRecommendedCommunities(repository),
        getPosts = GetCommunityPosts(postsRepository),
        authRepository = auth,
    )
}
