package org.bookie.app.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import org.bookie.app.presentation.components.BookieActionButton as Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import org.bookie.app.presentation.components.BookiePageHeading
import org.bookie.app.presentation.components.BookieFeatureBanner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.domain.model.Book
import org.bookie.app.presentation.library.LibraryActionState
import org.bookie.app.presentation.components.BookieEmptyState
import org.bookie.app.presentation.components.BookieErrorState
import org.bookie.app.presentation.components.BookieLoadingState
import org.bookie.app.presentation.components.BookieTextField

@Composable
fun MoodSearchScreen(
    viewModel: MoodSearchViewModel,
    modifier: Modifier = Modifier,
    reviewsViewModel: BookReviewsViewModel? = null,
    onSignIn: (() -> Unit)? = null,
    onRateBook: ((Book, Int) -> Unit)? = null,
    libraryAction: LibraryActionState? = null,
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val selected = (state as? MoodSearchUiState.Success)?.selectedBook

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (selected == null) BookiePageHeading("¿Cómo te sentís hoy?", "Hay una historia para cada momento.")

        if (selected != null) {
            // Reuses #3's existing catalog detail instead of duplicating its fields/links.
            BookDetail(
                book = selected.book,
                onBack = viewModel::showResults,
                modifier = Modifier.weight(1f),
                reviewsViewModel = reviewsViewModel,
                onRateBook = onRateBook,
                libraryAction = libraryAction,
            )
            when (val currentSave = saveState) {
                MoodSaveUiState.Idle -> Unit
                MoodSaveUiState.Saving -> Text("Guardando libro...")
                MoodSaveUiState.Saved -> Text("Libro guardado en tu biblioteca.")
                MoodSaveUiState.SignInRequired -> Text(
                    "Iniciá sesión para guardar este libro en tu biblioteca.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is MoodSaveUiState.Error -> Text(
                    currentSave.message,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = viewModel::saveSelectedBook,
                enabled = saveState !is MoodSaveUiState.Saving &&
                    saveState !is MoodSaveUiState.Saved,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Guardar en mi biblioteca")
            }
            if (saveState is MoodSaveUiState.SignInRequired) {
                onSignIn?.let { signIn ->
                    Button(onClick = signIn, modifier = Modifier.fillMaxWidth()) {
                        Text("Iniciar sesión")
                    }
                }
            }
        } else {
            BookieTextField(
                value = query,
                onValueChange = viewModel::onQueryChanged,
                label = "¿Qué tenés ganas de leer?",
                placeholder = "Ej: Una aventura fantástica y emocionante",
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = viewModel::search,
                enabled = query.isNotBlank() && state !is MoodSearchUiState.Loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Descubrir libros")
            }
            when (val current = state) {
                MoodSearchUiState.Idle ->
                    BookieFeatureBanner("Dejá que una historia te encuentre.",
                        "Probá con: una aventura que me sorprenda, algo para reír o una historia para desconectar.")
                MoodSearchUiState.Loading ->
                    BookieLoadingState(message = "Buscando libros para vos...")
                MoodSearchUiState.Empty ->
                    BookieEmptyState(message = "No encontramos libros para esta búsqueda.")
                is MoodSearchUiState.Error ->
                    BookieErrorState(message = current.message, onRetry = viewModel::search)
                is MoodSearchUiState.Success -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(current.books, key = { it.book.id }) { result ->
                        org.bookie.app.presentation.components.BookieCatalogCard(result.book,
                            onClick = { viewModel.selectBook(result) })
                    }
                }
            }
        }
    }
}
