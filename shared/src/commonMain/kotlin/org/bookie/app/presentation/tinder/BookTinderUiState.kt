package org.bookie.app.presentation.tinder

import org.bookie.app.domain.model.BookTinderFeed
import org.bookie.app.domain.model.RecommendationSource

sealed interface BookTinderUiState {
    data object Loading : BookTinderUiState
    data class Ready(
        val feed: BookTinderFeed,
        val currentIndex: Int = 0,
        val submitting: Boolean = false,
        val actionError: String? = null,
    ) : BookTinderUiState
    data class Empty(val source: RecommendationSource) : BookTinderUiState
    data class Exhausted(val source: RecommendationSource) : BookTinderUiState
    data class Error(val message: String) : BookTinderUiState
    data class SignInRequired(val message: String) : BookTinderUiState
}
