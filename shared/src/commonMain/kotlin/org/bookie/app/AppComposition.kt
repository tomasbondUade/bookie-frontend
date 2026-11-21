package org.bookie.app

import io.ktor.client.HttpClient
import org.bookie.app.data.auth.AuthClientBundle
import org.bookie.app.data.local.LibraryCache
import org.bookie.app.data.remote.BookieHttpClientConfig
import org.bookie.app.data.remote.KtorBookReviewsRemoteDataSource
import org.bookie.app.data.remote.KtorBookSearchRemoteDataSource
import org.bookie.app.data.remote.KtorReadingStatsRemoteDataSource
import org.bookie.app.data.remote.KtorSeriesReadingRemoteDataSource
import org.bookie.app.data.remote.createPlatformHttpClient
import org.bookie.app.data.repository.BookReviewsRepositoryImpl
import org.bookie.app.data.repository.BookRepositoryImpl
import org.bookie.app.data.repository.ReadingStatsRepositoryImpl
import org.bookie.app.data.repository.SeriesReadingRepositoryImpl
import org.bookie.app.domain.usecase.GetActiveSeries
import org.bookie.app.domain.usecase.GetBookReviews
import org.bookie.app.domain.usecase.SearchBooks
import org.bookie.app.domain.usecase.GetReadingStats
import org.bookie.app.domain.usecase.GetSeriesCatalog
import org.bookie.app.domain.usecase.GetSeriesProgress
import org.bookie.app.domain.usecase.GetSeriesReadingMap
import org.bookie.app.domain.usecase.GetUniverseCatalog
import org.bookie.app.domain.usecase.GetUniverseDetail
import org.bookie.app.presentation.navigation.AppFeatureViewModels
import org.bookie.app.presentation.profile.ReadingStatsViewModel
import org.bookie.app.presentation.search.BookReviewsViewModel
import org.bookie.app.presentation.search.SearchViewModel
import org.bookie.app.presentation.series.SeriesExplorerViewModel
import org.bookie.app.presentation.series.SeriesReadingViewModel

class AppRuntime internal constructor(
    val featureViewModels: AppFeatureViewModels,
    private val closeClients: () -> Unit,
) {
    fun close() = closeClients()
}

fun createAppRuntime(
    config: AppRuntimeConfig,
    cache: LibraryCache,
    authClients: AuthClientBundle? = null,
): AppRuntime? {
    if (!config.hasApi) return null

    val ownsClient = authClients == null
    val client = authClients?.bookieClient ?: createPlatformHttpClient(
        BookieHttpClientConfig(baseUrl = config.apiBaseUrl),
    )
    val features = createAppFeatureViewModels(client, cache, authClients)
    return AppRuntime(features) {
        if (ownsClient) client.close() else authClients?.close()
    }
}

private fun createAppFeatureViewModels(
    client: HttpClient,
    cache: LibraryCache,
    authClients: AuthClientBundle?,
    existingAuth: org.bookie.app.presentation.auth.AuthViewModel? = null,
    includeAccountFactory: Boolean = true,
): AppFeatureViewModels {
    val series = SeriesReadingRepositoryImpl(KtorSeriesReadingRemoteDataSource(client))
    val auth = existingAuth ?: authClients?.let { createAuthViewModel(it, cache) }

    return AppFeatureViewModels(
        readingStats = ReadingStatsViewModel(
            GetReadingStats(ReadingStatsRepositoryImpl(KtorReadingStatsRemoteDataSource(client))),
        ),
        seriesExplorer = SeriesExplorerViewModel(
            GetSeriesCatalog(series),
            GetUniverseCatalog(series),
            GetUniverseDetail(series),
        ),
        seriesReading = SeriesReadingViewModel(
            GetActiveSeries(series),
            GetSeriesProgress(series),
            GetSeriesReadingMap(series),
        ),
        onSignIn = null,
        moodSearch = authClients?.let(::createAuthenticatedMoodSearchViewModel),
        bookSearch = SearchViewModel(SearchBooks(BookRepositoryImpl(KtorBookSearchRemoteDataSource(client)))),
        bookReviews = BookReviewsViewModel(
            GetBookReviews(BookReviewsRepositoryImpl(KtorBookReviewsRemoteDataSource(client))),
        ),
        community = authClients?.let(::createAuthenticatedCommunityViewModel),
        createCommunityPostsViewModel = authClients?.let { bundle ->
            { communityId -> createAuthenticatedCommunityPostsViewModel(bundle, communityId) }
        },
        auth = auth,
        readingPreferences = authClients?.let(::createAuthenticatedReadingPreferencesViewModel),
        libraryExperience = authClients?.let { createAuthenticatedLibraryExperienceViewModel(it, cache) },
        createReadingProgressViewModel = authClients?.let { bundle ->
            { entry -> createAuthenticatedReadingProgressViewModel(bundle, cache, entry) }
        },
        bookLists = authClients?.let(::createAuthenticatedBookListsViewModel),
        bookTinder = authClients?.let(::createAuthenticatedBookTinderViewModel),
        readingCompatibility = authClients?.let(::createAuthenticatedReadingCompatibilityViewModel),
        readingChallenges = authClients?.let(::createAuthenticatedReadingChallengesViewModel),
        forAccount = if (includeAccountFactory && authClients != null) {
            { userId ->
                require(userId.isNotBlank())
                createAppFeatureViewModels(
                    client = client,
                    cache = cache,
                    authClients = authClients,
                    existingAuth = auth,
                    includeAccountFactory = false,
                )
            }
        } else null,
    )
}
