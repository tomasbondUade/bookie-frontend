package org.bookie.app.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.model.LibraryStateInput
import org.bookie.app.domain.model.LibraryStatus
import org.bookie.app.presentation.components.BookCover
import org.bookie.app.presentation.components.LibraryDatePickerDialog
import org.bookie.app.presentation.components.LibraryNumberPickerDialog

@Composable
fun LibraryExperienceScreen(
    viewModel: LibraryExperienceViewModel,
    modifier: Modifier = Modifier,
    onOpenProgress: ((LibraryBookState) -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

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
        Text("BIBLIOTECA", style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        LibraryFilters(state.statusFilter, state.favoritesOnly, viewModel)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Buscar en mi biblioteca") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        LibraryActionMessage(state.action, onDismiss = viewModel::dismissAction)

        when (val content = state.content) {
            LibraryContentState.Idle,
            is LibraryContentState.Loading -> {
                CircularProgressIndicator()
                Text("Cargando tu biblioteca")
                if (content is LibraryContentState.Loading && content.previous != null) {
                    content.previous.entries.filter { it.matches(query) }.forEach {
                        LibraryEntry(it, viewModel, onOpenProgress)
                    }
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
                val visible = content.snapshot.entries.filter { it.matches(query) }
                if (visible.isEmpty() && query.isNotBlank()) {
                    Text("No hay libros con ese título o autor en esta página.")
                }
                visible.forEach { LibraryEntry(it, viewModel, onOpenProgress) }
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
private fun LibraryEntry(
    entry: LibraryBookState,
    viewModel: LibraryExperienceViewModel,
    onOpenProgress: ((LibraryBookState) -> Unit)?,
) {
    var editing by remember(entry.id) { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCover(entry.book, Modifier.size(width = 72.dp, height = 96.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.book.title, style = MaterialTheme.typography.titleMedium)
                Text(entry.book.authors.joinToString().ifBlank { "Autor desconocido" })
                Text(entry.status.label(), style = MaterialTheme.typography.labelMedium)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(entry.status.label())
            entry.rating?.let { Text("$it/5") }
            if (entry.isFavorite) Text("Favorito")
            if (entry.isAbandoned) Text("Abandonado")
        }
        entry.progressPercentage?.let {
            Text("Progreso: $it%")
            LinearProgressIndicator(progress = { it / 100f }, modifier = Modifier.fillMaxWidth())
        }
        entry.currentPage?.let { Text("Página: $it") }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            if (entry.status == LibraryStatus.READING) {
                onOpenProgress?.let { open ->
                    TextButton(onClick = { open(entry) }) { Text("Registrar progreso") }
                }
            }
            TextButton(onClick = { viewModel.requestRemoval(entry.id) }) { Text("Quitar de biblioteca") }
        }
    }

    if (editing) {
        LibraryEntryEditorDialog(entry, viewModel, onDismiss = { editing = false })
    }
}

private fun LibraryBookState.matches(query: String): Boolean {
    val text = query.trim()
    return text.isEmpty() || book.title.contains(text, ignoreCase = true) ||
        book.authors.any { it.contains(text, ignoreCase = true) }
}

@Composable
private fun LibraryEntryEditorDialog(
    entry: LibraryBookState,
    viewModel: LibraryExperienceViewModel,
    onDismiss: () -> Unit,
) {
    var status by remember(entry.id) { mutableStateOf(entry.status) }
    var rating by remember(entry.id) { mutableStateOf(entry.rating) }
    var comment by remember(entry.id) { mutableStateOf(entry.comment.orEmpty()) }
    var privateNotes by remember(entry.id) { mutableStateOf(entry.privateNotes.orEmpty()) }
    var startedAt by remember(entry.id) { mutableStateOf(entry.startedAt.orEmpty()) }
    var finishedAt by remember(entry.id) { mutableStateOf(entry.finishedAt.orEmpty()) }
    var showRatingPicker by remember(entry.id) { mutableStateOf(false) }
    var showStartedAtPicker by remember(entry.id) { mutableStateOf(false) }
    var showFinishedAtPicker by remember(entry.id) { mutableStateOf(false) }
    var isFavorite by remember(entry.id) { mutableStateOf(entry.isFavorite) }
    var isAbandoned by remember(entry.id) { mutableStateOf(entry.isAbandoned) }
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
                OutlinedButton(
                    onClick = { showRatingPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(rating?.let { "Calificación: $it de 5" } ?: "Elegir calificación") }
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
                OutlinedButton(
                    onClick = { showStartedAtPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(startedAt.dateLabel("Elegir fecha de inicio")) }
                OutlinedButton(
                    onClick = { showFinishedAtPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(finishedAt.dateLabel("Elegir fecha de finalización")) }
                ToggleRow("Favorito", isFavorite) { isFavorite = it }
                ToggleRow("Abandonado", isAbandoned) { isAbandoned = it }
                if (saveStarted && action is LibraryActionState.Failed) {
                    Text(action.message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
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

    if (showRatingPicker) {
        LibraryNumberPickerDialog(
            title = "Calificación (1 a 5)",
            initialValue = rating,
            minValue = 1,
            maxValue = 5,
            allowEmpty = true,
            onDismiss = { showRatingPicker = false },
            onConfirm = { rating = it; showRatingPicker = false },
        )
    }
    if (showStartedAtPicker) {
        LibraryDatePickerDialog(
            title = "Fecha de inicio",
            initialValue = startedAt,
            onDismiss = { showStartedAtPicker = false },
            onConfirm = { startedAt = it.orEmpty(); showStartedAtPicker = false },
        )
    }
    if (showFinishedAtPicker) {
        LibraryDatePickerDialog(
            title = "Fecha de finalización",
            initialValue = finishedAt,
            onDismiss = { showFinishedAtPicker = false },
            onConfirm = { finishedAt = it.orEmpty(); showFinishedAtPicker = false },
        )
    }
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

private fun String.dateLabel(emptyLabel: String): String =
    takeIf(String::isNotBlank)?.take(10) ?: emptyLabel
