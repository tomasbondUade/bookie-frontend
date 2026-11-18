package org.bookie.app.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.domain.model.BookReview
import org.bookie.app.domain.model.BookReviewSort
import org.bookie.app.presentation.components.BookieEmptyState
import org.bookie.app.presentation.components.BookieErrorState
import org.bookie.app.presentation.components.BookieLoadingState

/** Renders inside the existing scrollable BookDetail; don't nest LazyColumn. */
@Composable
internal fun BookReviewsSection(
    bookId: String,
    viewModel: BookReviewsViewModel,
) {
    LaunchedEffect(viewModel, bookId) { viewModel.selectBook(bookId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Reseñas de lectores", style = MaterialTheme.typography.titleLarge)
        when (val current = state) {
            BookReviewsUiState.Idle, BookReviewsUiState.Loading ->
                BookieLoadingState(message = "Cargando reseñas...")
            BookReviewsUiState.Empty ->
                BookieEmptyState(message = "Este libro todavía no tiene reseñas.")
            is BookReviewsUiState.Error ->
                BookieErrorState(message = current.message, onRetry = viewModel::retry)
            is BookReviewsUiState.Ready -> {
                Text("${current.total} reseña(s)", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { viewModel.changeSort(BookReviewSort.RECENT) },
                        enabled = current.sort != BookReviewSort.RECENT,
                    ) { Text("Recientes") }
                    TextButton(
                        onClick = { viewModel.changeSort(BookReviewSort.TOP) },
                        enabled = current.sort != BookReviewSort.TOP,
                    ) { Text("Mejor puntuadas") }
                }
                current.reviews.forEach { review ->
                    ReviewCard(review)
                }
                current.pageError?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
                if (current.hasMore) {
                    Button(
                        onClick = viewModel::loadMore,
                        enabled = !current.loadingMore,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (current.loadingMore) "Cargando..." else "Ver más reseñas")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(review: BookReview) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(review.username, style = MaterialTheme.typography.titleSmall)
            review.rating?.let { rating ->
                Text("Calificación: $rating/5", style = MaterialTheme.typography.bodyMedium)
            }
            review.comment?.takeIf(String::isNotBlank)?.let { comment ->
                Text(comment, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                review.updatedAt.substringBefore('T'),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
