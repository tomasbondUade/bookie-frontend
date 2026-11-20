package org.bookie.app.presentation.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.model.LibraryStateInput
import org.bookie.app.domain.model.LibraryStatus
import org.bookie.app.presentation.components.BookieEmptyState
import org.bookie.app.presentation.components.BookieErrorState
import org.bookie.app.presentation.components.BookieLoadingState

/** UI for #32. Compose never accesses Ktor, DTOs, SQLDelight or a JWT. */
@Composable
fun LibraryScreen(
    viewModel: LibraryExperienceViewModel,
    onExploreBooks: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.refresh() }
    val writing = state.action is LibraryActionState.Working

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Mi biblioteca", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onExploreBooks) { Text("Explorar libros") }
            TextButton(onClick = viewModel::refresh, enabled = !writing) {
                Text("Actualizar")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = state.statusFilter == null,
                onClick = { viewModel.setStatusFilter(null) },
                label = { Text("Todos") },
            )
            LibraryStatus.entries.forEach { status ->
                FilterChip(
                    selected = state.statusFilter == status,
                    onClick = { viewModel.setStatusFilter(status) },
                    label = { Text(status.libraryLabel()) },
                )
            }
        }
        FilterChip(
            selected = state.favoritesOnly,
            onClick = { viewModel.setFavoritesOnly(!state.favoritesOnly) },
            label = { Text("Solo favoritos") },
        )
        when (val action = state.action) {
            LibraryActionState.Idle -> Unit
            is LibraryActionState.Working -> Text(action.label)
            is LibraryActionState.Succeeded ->
                LibraryNotice(action.message, false, viewModel::dismissAction)
            is LibraryActionState.Failed ->
                LibraryNotice(action.message, true, viewModel::dismissAction)
        }
        when (val content = state.content) {
            LibraryContentState.Idle ->
                BookieLoadingState(message = "Preparando biblioteca...")
            is LibraryContentState.Loading -> {
                if (content.previous == null) {
                    BookieLoadingState(message = "Actualizando biblioteca...")
                } else {
                    Text("Actualizando desde el servidor...")
                    LibraryResults(content.previous.copy(stale = true), writing, viewModel)
                }
            }
            is LibraryContentState.Ready -> LibraryResults(content.snapshot, writing, viewModel)
            is LibraryContentState.Empty -> {
                if (content.stale) {
                    Text("Datos guardados localmente; podrían estar desactualizados.")
                }
                content.warning?.let { message ->
                    LibraryNotice(message, true, null)
                    TextButton(onClick = viewModel::retry) { Text("Reintentar") }
                }
                BookieEmptyState(message = "Todavía no tenés libros en esta selección.")
            }
            is LibraryContentState.Error ->
                BookieErrorState(message = content.message, onRetry = viewModel::retry)
        }
    }
}

@Composable
fun LibraryNotConnectedScreen(
    onSignIn: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Mi biblioteca", style = MaterialTheme.typography.headlineSmall)
        Text("Iniciá sesión para consultar tus libros.")
        Text(
            "Esta sección necesita la sesión de tu cuenta. No muestra datos de otros usuarios.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onSignIn != null) {
            Button(onClick = onSignIn) { Text("Iniciar sesión") }
        }
    }
}

@Composable
private fun LibraryResults(
    snapshot: LibrarySnapshot,
    writing: Boolean,
    viewModel: LibraryExperienceViewModel,
) {
    if (snapshot.stale) {
        Text(
            "Mostrando datos cacheados. Podrían estar desactualizados.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    snapshot.warning?.let { message ->
        LibraryNotice(message, true, null)
        TextButton(onClick = viewModel::retry) { Text("Reintentar") }
    }
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(snapshot.entries, key = LibraryBookState::id) { entry ->
            LibraryBookCard(entry, writing, viewModel)
        }
        if (snapshot.canLoadMore) {
            item {
                Button(
                    onClick = viewModel::loadNextPage,
                    enabled = !snapshot.isLoadingMore && !writing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (snapshot.isLoadingMore) "Cargando..." else "Ver más libros")
                }
            }
        }
    }
}

@Composable
private fun LibraryBookCard(
    entry: LibraryBookState,
    writing: Boolean,
    viewModel: LibraryExperienceViewModel,
) {
    var pageDraft by remember(entry.id, entry.updatedAt) { mutableStateOf("") }
    var percentageDraft by remember(entry.id, entry.updatedAt) { mutableStateOf("") }
    var localError by remember(entry.id, entry.updatedAt) { mutableStateOf<String?>(null) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(entry.book.title, style = MaterialTheme.typography.titleMedium)
            if (entry.book.authors.isNotEmpty()) {
                Text(entry.book.authors.joinToString(", "), style = MaterialTheme.typography.bodySmall)
            }
            Text("Estado actual: " + entry.status.libraryLabel())
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LibraryStatus.entries.forEach { status ->
                    FilterChip(
                        selected = entry.status == status,
                        onClick = { viewModel.changeStatus(entry.bookId, status) },
                        enabled = !writing && entry.status != status,
                        label = { Text(status.libraryLabel()) },
                    )
                }
            }
            TextButton(
                onClick = {
                    viewModel.save(
                        LibraryStateInput(bookId = entry.bookId, isFavorite = !entry.isFavorite),
                        "Favoritos actualizados.",
                    )
                },
                enabled = !writing,
            ) {
                Text(if (entry.isFavorite) "★ Quitar favorito" else "☆ Marcar favorito")
            }
            Text(
                "Progreso: " + (entry.progressPercentage?.toString()?.plus("%") ?: "sin porcentaje") +
                    " · Página: " + (entry.currentPage?.toString() ?: "sin registrar"),
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = pageDraft,
                onValueChange = { value ->
                    if (value.all(Char::isDigit)) { pageDraft = value; localError = null }
                },
                label = { Text("Nueva página (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !writing,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = percentageDraft,
                onValueChange = { value ->
                    if (value.all(Char::isDigit)) { percentageDraft = value; localError = null }
                },
                label = { Text("Nuevo porcentaje 0-100 (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !writing,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            localError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                enabled = !writing,
                onClick = {
                    val page = pageDraft.toIntOrNull()
                    val percentage = percentageDraft.toIntOrNull()
                    if ((page == null && percentage == null) ||
                        (pageDraft.isNotEmpty() && page == null) ||
                        (percentageDraft.isNotEmpty() && percentage == null) ||
                        (percentage != null && percentage !in 0..100)
                    ) {
                        localError = "Ingresá una página válida o un porcentaje entre 0 y 100."
                    } else {
                        viewModel.updateProgress(entry.bookId, page, percentage)
                    }
                },
            ) { Text("Guardar progreso") }
        }
    }
}

@Composable
private fun LibraryNotice(message: String, error: Boolean, onDismiss: (() -> Unit)?) {
    Text(
        message,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    )
    if (onDismiss != null) {
        TextButton(onClick = onDismiss) { Text("Cerrar aviso") }
    }
}

internal fun LibraryStatus.libraryLabel(): String = when (this) {
    LibraryStatus.TO_READ -> "Pendiente"
    LibraryStatus.READING -> "Leyendo"
    LibraryStatus.READ -> "Terminado"
}
