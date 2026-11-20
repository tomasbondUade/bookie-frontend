package org.bookie.app.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.model.LibraryStateInput
import org.bookie.app.domain.model.LibraryStatus

@Composable
fun LibraryExperienceScreen(
    viewModel: LibraryExperienceViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        if (viewModel.uiState.value.content == LibraryContentState.Idle) viewModel.refresh()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Mi biblioteca", style = MaterialTheme.typography.headlineSmall)
        LibraryFilters(state.statusFilter, state.favoritesOnly, viewModel)
        LibraryActionMessage(state.action, onDismiss = viewModel::dismissAction)

        when (val content = state.content) {
            LibraryContentState.Idle,
            is LibraryContentState.Loading -> {
                CircularProgressIndicator()
                Text("Cargando tu biblioteca")
                if (content is LibraryContentState.Loading && content.previous != null) {
                    content.previous.entries.forEach { LibraryEntry(it, viewModel) }
                }
            }

            is LibraryContentState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(content.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = viewModel::retry) { Text("Reintentar") }
            }

            is LibraryContentState.Empty -> {
                Text(content.warning ?: if (content.stale) "No se pudo actualizar la biblioteca." else "Todavía no guardaste libros.")
                Button(onClick = viewModel::retry) { Text("Actualizar") }
            }

            is LibraryContentState.Ready -> {
                content.snapshot.warning?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (content.snapshot.stale) {
                    Text("Mostrando la última copia guardada mientras se actualiza.")
                }
                content.snapshot.entries.forEach { LibraryEntry(it, viewModel) }
                if (content.snapshot.canLoadMore) {
                    Button(
                        onClick = viewModel::loadNextPage,
                        enabled = !content.snapshot.isLoadingMore,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (content.snapshot.isLoadingMore) CircularProgressIndicator()
                        else Text("Cargar más")
                    }
                }
                TextButton(onClick = viewModel::refresh) { Text("Actualizar") }
            }
        }
    }

    state.pendingRemoval?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRemoval,
            title = { Text("Quitar de la biblioteca") },
            text = { Text("¿Querés quitar «${pending.book.title}» de tu biblioteca? El libro seguirá en el catálogo.") },
            confirmButton = {
                Button(onClick = viewModel::confirmRemoval) { Text("Quitar") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelRemoval) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun LibraryFilters(
    selectedStatus: LibraryStatus?,
    favoritesOnly: Boolean,
    viewModel: LibraryExperienceViewModel,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedStatus == null,
            onClick = { viewModel.setStatusFilter(null) },
            label = { Text("Todos") },
        )
        LibraryStatus.entries.forEach { status ->
            FilterChip(
                selected = selectedStatus == status,
                onClick = { viewModel.setStatusFilter(status) },
                label = { Text(status.label()) },
            )
        }
        FilterChip(
            selected = favoritesOnly,
            onClick = { viewModel.setFavoritesOnly(!favoritesOnly) },
            label = { Text("Favoritos") },
        )
    }
}

@Composable
private fun LibraryActionMessage(action: LibraryActionState, onDismiss: () -> Unit) {
    when (action) {
        LibraryActionState.Idle -> Unit
        is LibraryActionState.Working -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator()
            Text(action.label, modifier = Modifier.padding(start = 8.dp))
        }
        is LibraryActionState.Succeeded -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(action.message, color = MaterialTheme.colorScheme.primary)
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
        is LibraryActionState.Failed -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(action.message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    }
}

@Composable
private fun LibraryEntry(entry: LibraryBookState, viewModel: LibraryExperienceViewModel) {
    var editing by remember(entry.id) { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(entry.book.title, style = MaterialTheme.typography.titleMedium)
        Text(entry.book.authors.joinToString().ifBlank { "Autor desconocido" })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(entry.status.label())
            entry.rating?.let { Text("$it/5") }
            if (entry.isFavorite) Text("Favorito")
            if (entry.isAbandoned) Text("Abandonado")
        }
        entry.progressPercentage?.let { Text("Progreso: $it%") }
        entry.currentPage?.let { Text("Página: $it") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = {
                    viewModel.save(
                        LibraryStateInput(entry.bookId, isFavorite = !entry.isFavorite),
                        if (entry.isFavorite) "Libro quitado de favoritos." else "Libro agregado a favoritos.",
                    )
                },
            ) { Text(if (entry.isFavorite) "Quitar favorito" else "Marcar favorito") }
            TextButton(onClick = {
                viewModel.dismissAction()
                editing = true
            }) { Text("Editar") }
            TextButton(onClick = { viewModel.requestRemoval(entry.id) }) { Text("Quitar de biblioteca") }
        }
    }

    if (editing) {
        LibraryEntryEditorDialog(entry, viewModel, onDismiss = { editing = false })
    }
}

@Composable
private fun LibraryEntryEditorDialog(
    entry: LibraryBookState,
    viewModel: LibraryExperienceViewModel,
    onDismiss: () -> Unit,
) {
    var status by remember(entry.id) { mutableStateOf(entry.status) }
    var ratingText by remember(entry.id) { mutableStateOf(entry.rating?.toString().orEmpty()) }
    var comment by remember(entry.id) { mutableStateOf(entry.comment.orEmpty()) }
    var privateNotes by remember(entry.id) { mutableStateOf(entry.privateNotes.orEmpty()) }
    var startedAt by remember(entry.id) { mutableStateOf(entry.startedAt.orEmpty()) }
    var finishedAt by remember(entry.id) { mutableStateOf(entry.finishedAt.orEmpty()) }
    var isFavorite by remember(entry.id) { mutableStateOf(entry.isFavorite) }
    var isAbandoned by remember(entry.id) { mutableStateOf(entry.isAbandoned) }
    var error by remember(entry.id) { mutableStateOf<String?>(null) }
    var saveStarted by remember(entry.id) { mutableStateOf(false) }
    val screenState by viewModel.uiState.collectAsStateWithLifecycle()
    val action = screenState.action

    LaunchedEffect(action, saveStarted) {
        if (saveStarted && action is LibraryActionState.Succeeded) onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar ${entry.book.title}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Estado", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    LibraryStatus.entries.forEach { option ->
                        FilterChip(
                            selected = status == option,
                            onClick = { status = option },
                            label = { Text(option.label()) },
                        )
                    }
                }
                OutlinedTextField(
                    value = ratingText,
                    onValueChange = { ratingText = it.filter(Char::isDigit).take(1) },
                    label = { Text("Calificación (1 a 5)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comentario") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                OutlinedTextField(
                    value = privateNotes,
                    onValueChange = { privateNotes = it },
                    label = { Text("Notas privadas") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                OutlinedTextField(
                    value = startedAt,
                    onValueChange = { startedAt = it },
                    label = { Text("Inicio (fecha ISO 8601)") },
                    placeholder = { Text("2026-11-19T10:00:00Z") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = finishedAt,
                    onValueChange = { finishedAt = it },
                    label = { Text("Finalización (fecha ISO 8601)") },
                    placeholder = { Text("2026-11-19T10:00:00Z") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ToggleRow("Favorito", isFavorite) { isFavorite = it }
                ToggleRow("Abandonado", isAbandoned) { isAbandoned = it }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (saveStarted && action is LibraryActionState.Failed) {
                    Text(action.message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val rating = ratingText.toIntOrNull()
                if (ratingText.isNotBlank() && (rating == null || rating !in 1..5)) {
                    error = "La calificación debe estar entre 1 y 5."
                    return@Button
                }
                if (startedAt.isNotBlank() && !startedAt.isIsoDateTime() ||
                    finishedAt.isNotBlank() && !finishedAt.isIsoDateTime()
                ) {
                    error = "Ingresá las fechas en formato ISO 8601."
                    return@Button
                }
                error = null
                saveStarted = true
                viewModel.save(
                    LibraryStateInput(
                        bookId = entry.bookId,
                        status = status,
                        rating = rating,
                        comment = comment,
                        isFavorite = isFavorite,
                        startedAt = startedAt.takeIf(String::isNotBlank),
                        finishedAt = finishedAt.takeIf(String::isNotBlank),
                        isAbandoned = isAbandoned,
                        privateNotes = privateNotes,
                    ),
                    "Datos personales guardados.",
                )
            }, enabled = action !is LibraryActionState.Working) {
                if (action is LibraryActionState.Working) CircularProgressIndicator()
                else Text("Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}

private fun LibraryStatus.label(): String = when (this) {
    LibraryStatus.TO_READ -> "Por leer"
    LibraryStatus.READING -> "Leyendo"
    LibraryStatus.READ -> "Leído"
}

private fun String.isIsoDateTime(): Boolean =
    Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:?\\d{2})$").matches(this)
