package org.bookie.app

import org.bookie.app.data.auth.AuthClientBundle
import org.bookie.app.data.remote.KtorBookSearchRemoteDataSource
import org.bookie.app.data.remote.KtorReadingPreferencesRemoteDataSource
import org.bookie.app.data.repository.BookRepositoryImpl
import org.bookie.app.data.repository.ReadingPreferencesRepositoryImpl
import org.bookie.app.domain.usecase.GetReadingPreferences
import org.bookie.app.domain.usecase.SaveReadingPreferences
import org.bookie.app.domain.usecase.SearchBooks
import org.bookie.app.presentation.preferences.ReadingPreferencesViewModel

/** Reuses the authenticated Bookie client supplied by the application composition root. */
fun createAuthenticatedReadingPreferencesViewModel(
    authClients: AuthClientBundle,
): ReadingPreferencesViewModel {
    val preferencesRepository = ReadingPreferencesRepositoryImpl(
        KtorReadingPreferencesRemoteDataSource(authClients.bookieClient),
    )
    val booksRepository = BookRepositoryImpl(
        KtorBookSearchRemoteDataSource(authClients.bookieClient),
    )
    return ReadingPreferencesViewModel(
        getPreferences = GetReadingPreferences(preferencesRepository),
        savePreferences = SaveReadingPreferences(preferencesRepository),
        searchBooks = SearchBooks(booksRepository),
    )
}
