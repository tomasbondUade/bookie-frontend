package org.bookie.app.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.presentation.components.BookieBookCard
import org.bookie.app.presentation.components.BookieEmptyState
import org.bookie.app.presentation.components.BookieErrorState
import org.bookie.app.presentation.components.BookieLoadingState
import org.bookie.app.presentation.components.BookieTextField

@Composable
fun MoodSearchScreen(
    viewModel: MoodSearchViewModel,
    modifier: Modifier = Modifier,
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val selected = (state as? MoodSearchUiState.Success)?.selectedBook

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Descubrí tu próxima lectura", style = MaterialTheme.typography.headlineSmall)

        if (selected != null) {
            // Reuses #3's existing catalog detail instead of duplicating its fields/links.
            BookDetail(
                book = selected.book,
                onBack = viewModel::showResults,
                modifier = Modifier.weight(1f),
            )
            when (val currentSave = saveState) {
                MoodSaveUiState.Idle -> Unit
                MoodSaveUiState.Saving -> Text("Guardando libro...")
                MoodSaveUiState.Saved -> Text("Libro guardado en tu biblioteca.")
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
                    Text("Describí tu estado de ánimo o la historia que querés encontrar.")
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
                        BookieBookCard(
                            title = result.book.title,
                            author = result.book.authors.joinToString()
                                .ifBlank { "Autor desconocido" },
                            onClick = { viewModel.selectBook(result) },
                        )
                    }
                }
            }
        }
    }
}
