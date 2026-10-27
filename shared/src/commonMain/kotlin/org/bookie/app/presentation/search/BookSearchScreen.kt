package org.bookie.app.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.domain.model.Book

@Composable
fun BookSearchScreen(
    viewModel: SearchViewModel,
    modifier: Modifier = Modifier,
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedBook = (state as? SearchUiState.Success)?.selectedBook

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Buscar libros", style = MaterialTheme.typography.headlineSmall)

        if (selectedBook == null) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Título o autor") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
            )
            Button(
                onClick = viewModel::search,
                enabled = query.isNotBlank() && state !is SearchUiState.Loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Buscar")
            }
        }

        when (val currentState = state) {
            SearchUiState.Idle -> Text("Ingresá un título o autor para buscar.")
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
            Text(book.title, style = MaterialTheme.typography.titleMedium)
            Text(book.authors.joinToString().ifBlank { "Autor desconocido" })
            if (book.genres.isNotEmpty()) {
                Text(book.genres.joinToString(), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BookDetail(
    book: Book,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) {
            Text("Volver a resultados")
        }
        Text(book.title, style = MaterialTheme.typography.headlineSmall)
        DetailField("Autores", book.authors.joinToString())
        DetailField("Géneros", book.genres.joinToString())
        book.description?.takeIf(String::isNotBlank)?.let { DetailField("Descripción", it) }
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
