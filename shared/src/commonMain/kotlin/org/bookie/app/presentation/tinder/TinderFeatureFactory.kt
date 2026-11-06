package org.bookie.app.presentation.tinder

import io.ktor.client.HttpClient
import org.bookie.app.data.auth.AuthClientBundle
import org.bookie.app.data.remote.KtorTinderRemoteDataSource
import org.bookie.app.data.repository.BookTinderRepositoryImpl
import org.bookie.app.domain.usecase.GetTinderBooks
import org.bookie.app.domain.usecase.RegisterBookSwipe

/**
 * Build Tinder with the application's existing authenticated HttpClient.
 * The platform composition root owns the client lifecycle.
 */
fun createTinderViewModel(httpClient: HttpClient): TinderViewModel {
    val remote = KtorTinderRemoteDataSource(httpClient)
    val repository = BookTinderRepositoryImpl(remote)
    return TinderViewModel(
        getTinderBooks = GetTinderBooks(repository),
        registerSwipe = RegisterBookSwipe(repository)
    )
}

/** Reuse the existing Supabase-backed JWT client and its refresh handling. */
fun createTinderViewModel(bundle: AuthClientBundle): TinderViewModel =
    createTinderViewModel(bundle.bookieClient)
