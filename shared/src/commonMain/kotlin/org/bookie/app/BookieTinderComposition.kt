package org.bookie.app

import org.bookie.app.data.auth.AuthClientBundle
import org.bookie.app.data.remote.KtorBookTinderRemoteDataSource
import org.bookie.app.data.repository.BookTinderRepositoryImpl
import org.bookie.app.domain.usecase.GetBookTinderFeed
import org.bookie.app.domain.usecase.RecordBookSwipe
import org.bookie.app.presentation.tinder.BookTinderViewModel

/** Uses the JWT-aware Bookie client; the backend resolves the account from the token. */
fun createAuthenticatedBookTinderViewModel(authClients: AuthClientBundle): BookTinderViewModel {
    val repository = BookTinderRepositoryImpl(
        KtorBookTinderRemoteDataSource(authClients.bookieClient),
    )
    return BookTinderViewModel(
        getBookTinderFeed = GetBookTinderFeed(repository),
        recordBookSwipe = RecordBookSwipe(repository),
    )
}
