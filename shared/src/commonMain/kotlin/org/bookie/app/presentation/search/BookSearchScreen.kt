package org.bookie.app.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.domain.model.Book
import org.bookie.app.presentation.components.BookCover
import org.bookie.app.presentation.library.LibraryActionState

@Composable
fun BookSearchScreen(
    viewModel: SearchViewModel,
    modifier: Modifier = Modifier,
    reviewsViewModel: BookReviewsViewModel? = null,
    onAddToLibrary: ((Book) -> Unit)? = null,
    onRateBook: ((Book, Int) -> Unit)? = null,
    libraryAction: LibraryActionState? = null,
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedBook = (state as? SearchUiState.Success)?.selectedBook

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (selectedBook == null) {
            Text("DESCUBRIR", style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("Buscar libro", style = MaterialTheme.typography.labelLarge)
        }

        if (selectedBook == null) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Título, autor o ISBN") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
            )
            Button(
                onClick = viewModel::search,
                enabled = query.isNotBlank() && state !is SearchUiState.Loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Buscar libros")
            }
        }

        when (val currentState = state) {
            SearchUiState.Idle -> Text("Encontrá tu próxima lectura por título, autor o ISBN.")
            SearchUiState.Loading -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text("Buscando…")
                }
            }

            is SearchUiState.Success -> {
                if (currentState.selectedBook != null) {
                    BookDetail(
                        book = currentState.selectedBook,
                        onBack = viewModel::showResults,
                        modifier = Modifier.weight(1f),
                        reviewsViewModel = reviewsViewModel,
                        onAddToLibrary = onAddToLibrary,
                        onRateBook = onRateBook,
                        libraryAction = libraryAction,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(currentState.books, key = Book::id) { book ->
                            BookResult(book = book, onClick = { viewModel.selectBook(book) })
                        }
                        if (currentState.hasMore) {
                            item(key = "search-load-more") {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    currentState.loadMoreError?.let { error ->
                                        Text(error, color = MaterialTheme.colorScheme.error)
                                    }
                                    if (currentState.loadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    } else {
                                        TextButton(onClick = viewModel::loadMore) {
                                            Text(if (currentState.loadMoreError == null) "Cargar más libros" else "Reintentar")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is SearchUiState.Empty -> Text("No se encontraron libros para “${currentState.query}”.")
            is SearchUiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(currentState.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = viewModel::search) {
                    Text("Reintentar")
                }
            }
        }
    }
}

@Composable
private fun BookResult(
    book: Book,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                BookCover(book, Modifier.size(width = 96.dp, height = 140.dp))
            }
            Text(book.title, style = MaterialTheme.typography.titleMedium)
            Text(book.authors.joinToString().ifBlank { "Autor desconocido" })
            if (book.genres.isNotEmpty()) {
                Text(book.genres.joinToString(), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun BookDetail(
    book: Book,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    reviewsViewModel: BookReviewsViewModel? = null,
    onAddToLibrary: ((Book) -> Unit)? = null,
    onRateBook: ((Book, Int) -> Unit)? = null,
    libraryAction: LibraryActionState? = null,
) {
    val uriHandler = LocalUriHandler.current
    var showRating by remember(book.id) { mutableStateOf(false) }
    var rating by remember(book.id) { mutableIntStateOf(5) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) {
            Text("Volver")
        }
        Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
            BookCover(book, Modifier.size(width = 142.dp, height = 208.dp))
        }
        Text(book.title, style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(book.authors.joinToString().ifBlank { "Autor desconocido" },
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(book.genres.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        HorizontalDivider()
        book.description?.takeIf(String::isNotBlank)?.let { Text(it) }
        onAddToLibrary?.let { add ->
            Button(onClick = { add(book) }, modifier = Modifier.fillMaxWidth()) {
                Text("Guardar en mi biblioteca")
            }
        }
        onRateBook?.let {
            TextButton(onClick = { showRating = true }) { Text("Puntuar") }
        }
        when (libraryAction) {
            is LibraryActionState.Working -> Text(libraryAction.label)
            is LibraryActionState.Succeeded -> Text(libraryAction.message,
                color = MaterialTheme.colorScheme.primary)
            is LibraryActionState.Failed -> Text(libraryAction.message,
                color = MaterialTheme.colorScheme.error)
            else -> Unit
        }
        book.coverUrl?.takeIf(String::isHttpUrl)?.let { url ->
            TextButton(onClick = { uriHandler.openUri(url) }) {
                Text("Abrir portada")
            }
        }
        book.previewLink?.takeIf(String::isHttpUrl)?.let { url ->
            TextButton(onClick = { uriHandler.openUri(url) }) {
                Text("Vista previa")
            }
        }
        book.infoLink?.takeIf(String::isHttpUrl)?.let { url ->
            TextButton(onClick = { uriHandler.openUri(url) }) {
                Text("Más información")
            }
        }
        book.buyLink?.takeIf(String::isHttpUrl)?.let { url ->
            TextButton(onClick = { uriHandler.openUri(url) }) {
                Text("Comprar")
            }
        }
        reviewsViewModel?.let { viewModel ->
            Text("Reseñas", style = MaterialTheme.typography.titleMedium)
            BookReviewsSection(bookId = book.id, viewModel = viewModel)
        }
    }
    if (showRating && onRateBook != null) {
        AlertDialog(
            onDismissRequest = { showRating = false },
            title = { Text("Puntuar ${book.title}") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { value ->
                        FilterChip(
                            selected = rating == value,
                            onClick = { rating = value },
                            label = { Text("$value") },
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onRateBook(book, rating); showRating = false }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showRating = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    if (value.isNotBlank()) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(value)
        }
    }
}

private fun String.isHttpUrl(): Boolean {
    val separator = indexOf("://")
    if (separator <= 0 || any(Char::isWhitespace)) return false

    val scheme = substring(0, separator)
    if (!scheme.equals("http", ignoreCase = true) && !scheme.equals("https", ignoreCase = true)) {
        return false
    }

    val host = substring(separator + 3)
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
    return host.isNotBlank()
}
