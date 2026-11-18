package org.bookie.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import org.bookie.app.data.remote.BookieHttpClientConfig
import org.bookie.app.data.remote.KtorBookReviewsRemoteDataSource
import org.bookie.app.data.remote.createPlatformHttpClient
import org.bookie.app.data.repository.BookReviewsRepositoryImpl
import org.bookie.app.domain.usecase.GetBookReviews
import org.bookie.app.presentation.search.BookReviewsViewModel

/** Public reviews use the same platform API configuration as mood search. */
@Composable
internal fun rememberConfiguredBookReviewsViewModel(): BookReviewsViewModel? {
    val baseUrl = platformBookieApiBaseUrl()?.trim().orEmpty()
    if (baseUrl.isBlank()) return null

    val client = remember(baseUrl) {
        runCatching { createPlatformHttpClient(BookieHttpClientConfig(baseUrl = baseUrl)) }
            .getOrNull()
    } ?: return null

    DisposableEffect(client) {
        onDispose { client.close() }
    }

    return remember(client) {
        BookReviewsViewModel(
            GetBookReviews(
                BookReviewsRepositoryImpl(KtorBookReviewsRemoteDataSource(client)),
            ),
        )
    }
}
