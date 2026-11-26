package org.bookie.app.presentation.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import org.bookie.app.presentation.components.BookieActionButton as Button
import org.bookie.app.presentation.components.BookiePanelCard as Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import org.bookie.app.presentation.components.BookiePageHeading
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.model.LibraryStateInput
import org.bookie.app.domain.model.LibraryStatus
import org.bookie.app.presentation.components.BookieEmptyState
import org.bookie.app.presentation.components.BookieErrorState
import org.bookie.app.presentation.components.BookieLoadingState
import org.bookie.app.presentation.components.LibraryNumberPickerDialog

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
        BookiePageHeading("Mi biblioteca", "Un lugar para tus próximas historias.")
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
                    LibraryResults(content.previous.copy(stale = true), state.drafts, writing, viewModel)
                }
            }
            is LibraryContentState.Ready ->
                LibraryResults(content.snapshot, state.drafts, writing, viewModel)
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
        BookiePageHeading("Mi biblioteca", "Un lugar para tus próximas historias.")
        Text(if (onSignIn != null) {
            "Iniciá sesión para consultar tus libros."
        } else {
            "La conexión de cuenta todavía no está configurada en esta compilación."
        })
        Text(
            if (onSignIn != null) {
                "Esta sección necesita la sesión de tu cuenta. No muestra datos de otros usuarios."
            } else {
                "Configurá la URL y clave pública de Supabase para usar tu biblioteca. " +
                    "No se utilizan cuentas ni libros simulados."
            },
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
    drafts: Map<String, LibraryEditDraft>,
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
            LibraryBookCard(entry, drafts[entry.id], writing, viewModel)
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
    draft: LibraryEditDraft?,
    writing: Boolean,
    viewModel: LibraryExperienceViewModel,
) {
    val selectedStatus = draft?.status ?: entry.status
    val editableProgress = selectedStatus == LibraryStatus.READING
    val pageText = draft?.pageText ?: entry.currentPage?.toString().orEmpty()
    val percentageText = draft?.percentageText ?: entry.progressPercentage?.toString().orEmpty()
    var showPagePicker by remember(entry.id) { mutableStateOf(false) }
    var showPercentagePicker by remember(entry.id) { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                org.bookie.app.presentation.components.BookCover(entry.book, Modifier.size(60.dp, 86.dp))
                Text(entry.book.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            if (entry.book.authors.isNotEmpty()) {
                Text(entry.book.authors.joinToString(", "), style = MaterialTheme.typography.bodySmall)
            }
            Text("Estado actual: " + entry.status.libraryLabel())
            Text(
                "Elegí un estado; el cambio se confirma al tocar Guardar cambios.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LibraryStatus.entries.forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { viewModel.selectDraftStatus(entry.id, status) },
                        enabled = !writing,
                        label = { Text(status.libraryLabel()) },
                    )
                }
            }
            if (draft != null && draft.status != entry.status) {
                Text(
                    "Estado elegido: " + selectedStatus.libraryLabel() + " (sin guardar)",
                    style = MaterialTheme.typography.bodySmall,
                )
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
                "Progreso registrado: " +
                    (entry.progressPercentage?.toString()?.plus("%") ?: "sin porcentaje") +
                    " · Página: " + (entry.currentPage?.toString() ?: "sin registrar"),
                style = MaterialTheme.typography.bodySmall,
            )
            if (editableProgress) {
                Text(
                    "En estado Leyendo podés elegir una página o porcentaje antes de guardar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { showPagePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !writing,
                ) {
                    Text(pageText.takeIf(String::isNotBlank)?.let { "Página: $it" } ?: "Elegir página")
                }
                OutlinedButton(
                    onClick = { showPercentagePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !writing,
                ) {
                    Text(percentageText.takeIf(String::isNotBlank)?.let { "Progreso: $it%" } ?: "Elegir progreso")
                }
            } else {
                Text(
                    if (selectedStatus == LibraryStatus.READ) {
                        "Libro terminado: no se puede editar el progreso."
                    } else {
                        "Para editar el progreso, elegí Leyendo."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !writing && draft?.hasChangesFrom(entry) == true,
                    onClick = { viewModel.saveDraft(entry.id) },
                ) { Text("Guardar cambios") }
                if (draft != null) {
                    TextButton(
                        onClick = { viewModel.discardDraft(entry.id) },
                        enabled = !writing,
                    ) { Text("Cancelar") }
                }
            }
        }
    }

    if (showPagePicker) {
        LibraryNumberPickerDialog(
            title = "Página leída",
            initialValue = pageText.toIntOrNull(),
            minValue = 0,
            maxValue = MAX_PAGE,
            allowEmpty = true,
            onDismiss = { showPagePicker = false },
            onConfirm = {
                viewModel.editDraftPage(entry.id, it?.toString().orEmpty())
                showPagePicker = false
            },
        )
    }
    if (showPercentagePicker) {
        LibraryNumberPickerDialog(
            title = "Progreso (0 a 100%)",
            initialValue = percentageText.toIntOrNull(),
            minValue = 0,
            maxValue = 100,
            allowEmpty = true,
            onDismiss = { showPercentagePicker = false },
            onConfirm = {
                viewModel.editDraftPercentage(entry.id, it?.toString().orEmpty())
                showPercentagePicker = false
            },
        )
    }
}

private const val MAX_PAGE = 1_000_000

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
