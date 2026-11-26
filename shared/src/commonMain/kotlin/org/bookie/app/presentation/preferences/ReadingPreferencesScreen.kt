package org.bookie.app.presentation.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import org.bookie.app.presentation.components.BookieActionButton as Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import org.bookie.app.presentation.components.BookiePageHeading
import org.bookie.app.presentation.components.BookieInputShape
import org.bookie.app.presentation.components.bookieInputColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.domain.model.Book
import org.bookie.app.domain.model.ReadingPreferences

@Composable
fun ReadingPreferencesScreen(
    viewModel: ReadingPreferencesViewModel,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    onSignIn: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        if (viewModel.uiState.value == ReadingPreferencesUiState.Idle) viewModel.load()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (val current = state) {
            ReadingPreferencesUiState.Idle,
            ReadingPreferencesUiState.Loading -> {
                BookiePageHeading("Tus gustos lectores", "Hagamos lugar a lo que te encanta leer.")
                CircularProgressIndicator()
                Text("Cargando tus preferencias")
            }

            is ReadingPreferencesUiState.LoadError -> {
                Text(
                    if (current.sessionExpired) "Sesión vencida" else "No se pudieron cargar tus preferencias",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(current.message, color = MaterialTheme.colorScheme.error)
                if (current.sessionExpired && onSignIn != null) {
                    Button(onClick = onSignIn) { Text("Iniciar sesión") }
                } else {
                    Button(onClick = viewModel::retryLoad) { Text("Reintentar") }
                }
                onClose?.let { TextButton(onClick = it) { Text("Volver") } }
            }

            is ReadingPreferencesUiState.Viewing -> PreferencesSummary(
                preferences = current.preferences,
                notice = current.notice,
                onEdit = viewModel::beginEdit,
                onClose = onClose,
            )

            is ReadingPreferencesUiState.Editing -> PreferencesEditor(
                draft = current,
                saving = false,
                viewModel = viewModel,
            )

            is ReadingPreferencesUiState.Saving -> PreferencesEditor(
                draft = current.draft,
                saving = true,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun PreferencesSummary(
    preferences: ReadingPreferences,
    notice: String?,
    onEdit: () -> Unit,
    onClose: (() -> Unit)?,
) {
    BookiePageHeading("Tus gustos lectores", "Hagamos lugar a lo que te encanta leer.")
    notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    PreferenceValues("Géneros", preferences.preferredGenres)
    PreferenceValues("Autores", preferences.preferredAuthors)
    Text(
        if (preferences.favoriteBookIds.size == 1) "1 libro favorito" else "${preferences.favoriteBookIds.size} libros favoritos",
        style = MaterialTheme.typography.titleMedium,
    )
    Button(onClick = onEdit) { Text("Editar preferencias") }
    onClose?.let { TextButton(onClick = it) { Text("Volver al perfil") } }
}

@Composable
private fun PreferencesEditor(
    draft: ReadingPreferencesUiState.Editing,
    saving: Boolean,
    viewModel: ReadingPreferencesViewModel,
) {
    BookiePageHeading(if (draft.isOnboarding) "Tu mundo, tus libros" else "Tus gustos lectores",
        "Una selección tan única como vos.")
    Text("Tus preferencias se guardan en tu cuenta y se usan para personalizar recomendaciones.")

    Text("Géneros", style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            shape = BookieInputShape,
            colors = bookieInputColors(),
            value = draft.genreInput,
            onValueChange = viewModel::updateGenreInput,
            label = { Text("Agregar géneros") },
            placeholder = { Text("Fantasía, misterio...") },
            singleLine = true,
            enabled = !saving,
            modifier = Modifier.weight(1f),
        )
        Button(onClick = viewModel::addGenres, enabled = !saving && draft.genreInput.isNotBlank()) {
            Text("Agregar")
        }
    }
    draft.preferredGenres.forEach { genre ->
        RemovableValue(genre, enabled = !saving, onRemove = { viewModel.removeGenre(genre) })
    }

    Text("Autores", style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            shape = BookieInputShape,
            colors = bookieInputColors(),
            value = draft.authorInput,
            onValueChange = viewModel::updateAuthorInput,
            label = { Text("Agregar autores") },
            placeholder = { Text("Ursula K. Le Guin...") },
            singleLine = true,
            enabled = !saving,
            modifier = Modifier.weight(1f),
        )
        Button(onClick = viewModel::addAuthors, enabled = !saving && draft.authorInput.isNotBlank()) {
            Text("Agregar")
        }
    }
    draft.preferredAuthors.forEach { author ->
        RemovableValue(author, enabled = !saving, onRemove = { viewModel.removeAuthor(author) })
    }

    Text("Libros favoritos", style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            shape = BookieInputShape,
            colors = bookieInputColors(),
            value = draft.searchQuery,
            onValueChange = viewModel::updateSearchQuery,
            label = { Text("Buscar por título o autor") },
            singleLine = true,
            enabled = !saving,
            modifier = Modifier.weight(1f),
        )
        Button(onClick = viewModel::search, enabled = !saving && draft.searchQuery.isNotBlank()) {
            Text("Buscar")
        }
    }
    when (val search = draft.searchState) {
        BookSearchState.Idle -> Unit
        BookSearchState.Loading -> CircularProgressIndicator()
        BookSearchState.Empty -> Text("No se encontraron libros.")
        is BookSearchState.Error -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(search.message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = viewModel::retrySearch, enabled = !saving) { Text("Reintentar") }
        }
        is BookSearchState.Results -> search.books.forEach { book ->
            BookResult(
                book = book,
                selected = book.id in draft.favoriteBookIds,
                enabled = !saving,
                onToggle = { viewModel.toggleFavorite(book) },
            )
        }
    }

    if (draft.favoriteBookIds.isNotEmpty()) {
        Text("Libros elegidos: ${draft.favoriteBookIds.size}")
        draft.selectedBooks.forEach { book ->
            RemovableValue(
                "${book.title} · ${book.authors.joinToString()}",
                enabled = !saving,
                onRemove = { viewModel.toggleFavorite(book) },
            )
        }
        val unresolvedCount = draft.favoriteBookIds.size - draft.selectedBooks.size
        if (unresolvedCount > 0) {
            Text("Hay $unresolvedCount libro(s) guardado(s) anteriormente sin detalle disponible.")
            TextButton(onClick = viewModel::removeUnresolvedFavorites, enabled = !saving) {
                Text("Quitar libros sin detalle")
            }
        }
    }

    draft.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    Button(onClick = viewModel::save, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
        if (saving) CircularProgressIndicator()
        else Text(if (draft.isOnboarding) "Guardar y continuar" else "Guardar cambios")
    }
    if (!draft.isOnboarding) {
        TextButton(onClick = viewModel::cancelEdit, enabled = !saving) { Text("Cancelar") }
    }
}

@Composable
private fun PreferenceValues(label: String, values: List<String>) {
    org.bookie.app.presentation.components.BookiePanelCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(values.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "Sin preferencias elegidas",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RemovableValue(value: String, enabled: Boolean, onRemove: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(value, modifier = Modifier.weight(1f).padding(end = 8.dp))
        TextButton(onClick = onRemove, enabled = enabled) { Text("Quitar") }
    }
}

@Composable
private fun BookResult(book: Book, selected: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(book.title, style = MaterialTheme.typography.titleSmall)
            Text(book.authors.joinToString().ifBlank { "Autor desconocido" })
        }
        FilterChip(
            selected = selected,
            onClick = onToggle,
            enabled = enabled,
            label = { Text(if (selected) "Elegido" else "Elegir") },
        )
    }
}
