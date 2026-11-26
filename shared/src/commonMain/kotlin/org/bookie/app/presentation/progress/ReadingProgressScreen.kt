package org.bookie.app.presentation.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import org.bookie.app.presentation.components.BookieActionButton as Button
import org.bookie.app.presentation.components.BookiePanelCard as Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import org.bookie.app.presentation.components.BookiePageHeading
import org.bookie.app.presentation.components.BookieFeatureBanner
import org.bookie.app.presentation.components.BookieInputShape
import org.bookie.app.presentation.components.bookieInputColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.domain.model.ReadingSession
import org.bookie.app.presentation.components.LibraryNumberPickerDialog

@Composable
fun ReadingProgressScreen(
    viewModel: ReadingProgressViewModel,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPagePicker by remember { mutableStateOf(false) }
    var showPercentagePicker by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.loadSessions()
    }

    when (val current = state) {
        is ReadingProgressUiState.Unavailable -> Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
        ) {
            Text(current.message, color = MaterialTheme.colorScheme.error)
        }
        is ReadingProgressUiState.Content -> Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            onBack?.let { TextButton(onClick = it) { Text("Volver a biblioteca") } }
            BookiePageHeading("Tu lectura", "Un ratito de lectura, un paso más.")
            BookieFeatureBanner(current.bookTitle, "Tu progreso y tus notas, en un mismo lugar.")
            current.percentageInput.toIntOrNull()?.let { percentage ->
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { percentage.coerceIn(0, 100) / 100f }, modifier = Modifier.fillMaxWidth())
            }
            Text(
                "La página y el porcentaje son independientes: el catálogo no informa el total de páginas.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
                onClick = { showPagePicker = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !current.isBusy,
            ) {
                Text(current.currentPageInput.takeIf(String::isNotBlank)?.let { "Página actual: $it" } ?: "Elegir página")
            }
            current.currentPageError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedButton(
                onClick = { showPercentagePicker = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !current.isBusy,
            ) {
                Text(current.percentageInput.takeIf(String::isNotBlank)?.let { "Progreso: $it%" } ?: "Elegir porcentaje")
            }
            current.percentageError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Text("Guardar estado actual", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                shape = BookieInputShape,
                colors = bookieInputColors(),
                value = current.privateNotesInput,
                onValueChange = viewModel::onPrivateNotesChanged,
                label = { Text("Notas privadas del libro") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                enabled = !current.isBusy,
            )
            Button(
                onClick = viewModel::saveSnapshot,
                enabled = !current.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (current.snapshotStatus == ProgressOperationStatus.Loading) "Guardando…" else "Guardar progreso")
            }
            OperationFeedback(current.snapshotStatus)

            HorizontalDivider()
            Text("Registrar sesión de lectura", style = MaterialTheme.typography.titleMedium)
            Text(
                "Registrar una sesión no guarda automáticamente el estado actual; son operaciones separadas.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                shape = BookieInputShape,
                colors = bookieInputColors(),
                value = current.sessionNotesInput,
                onValueChange = viewModel::onSessionNotesChanged,
                label = { Text("Notas de esta sesión") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                enabled = !current.isBusy,
            )
            Button(
                onClick = viewModel::recordSession,
                enabled = !current.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when (current.sessionStatus) {
                        ProgressOperationStatus.Loading -> "Registrando…"
                        is ProgressOperationStatus.Error -> "Reintentar sesión"
                        else -> "Registrar sesión"
                    },
                )
            }
            OperationFeedback(current.sessionStatus)
            if (current.sessionStatus is ProgressOperationStatus.Error) {
                Text(
                    "Reintentar conserva la misma clave de operación. Iniciar otra puede duplicar una sesión si el servidor ya recibió la anterior.",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = viewModel::startNewSessionAttempt) {
                    Text("Descartar intento e iniciar otra sesión")
                }
            }

            HorizontalDivider()
            Text("Historial de sesiones", style = MaterialTheme.typography.titleMedium)
            if (current.historyLoading) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text("Cargando historial…")
                }
            }
            current.historyError?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = viewModel::loadSessions) { Text("Reintentar historial") }
            }
            if (!current.historyLoading && current.historyError == null && current.sessions.isEmpty()) {
                Text("Todavía no hay sesiones registradas.")
            }
            current.sessions.forEach { session -> SessionCard(session) }
        }
    }

    val progress = state as? ReadingProgressUiState.Content
    if (progress != null && showPagePicker) {
        LibraryNumberPickerDialog(
            title = "Página actual",
            initialValue = progress.currentPageInput.toIntOrNull(),
            minValue = 0,
            maxValue = 1_000_000,
            allowEmpty = true,
            onDismiss = { showPagePicker = false },
            onConfirm = {
                viewModel.onCurrentPageChanged(it?.toString().orEmpty())
                showPagePicker = false
            },
        )
    }
    if (progress != null && showPercentagePicker) {
        LibraryNumberPickerDialog(
            title = "Porcentaje de lectura",
            initialValue = progress.percentageInput.toIntOrNull(),
            minValue = 0,
            maxValue = 100,
            allowEmpty = true,
            onDismiss = { showPercentagePicker = false },
            onConfirm = {
                viewModel.onPercentageChanged(it?.toString().orEmpty())
                showPercentagePicker = false
            },
        )
    }
}

@Composable
private fun OperationFeedback(status: ProgressOperationStatus) {
    when (status) {
        ProgressOperationStatus.Idle -> Unit
        ProgressOperationStatus.Loading -> Text("Esperando confirmación del servidor…")
        is ProgressOperationStatus.Success -> Text(status.message, color = MaterialTheme.colorScheme.primary)
        is ProgressOperationStatus.Error -> Text(status.message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun SessionCard(session: ReadingSession) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(session.createdAt, style = MaterialTheme.typography.labelLarge)
            session.currentPage?.let { Text("Página: $it") }
            session.progressPercentage?.let { Text("Avance: $it%") }
            session.notes?.takeIf(String::isNotBlank)?.let { Text(it) }
        }
    }
}
