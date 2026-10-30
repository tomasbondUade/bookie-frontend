package org.bookie.app.presentation.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.domain.model.ReadingSession

@Composable
fun ReadingProgressScreen(
    viewModel: ReadingProgressViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
            Text("Progreso de lectura", style = MaterialTheme.typography.headlineSmall)
            Text(current.bookTitle, style = MaterialTheme.typography.titleMedium)
            Text(
                "La página y el porcentaje son independientes: el catálogo no informa el total de páginas.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = current.currentPageInput,
                onValueChange = viewModel::onCurrentPageChanged,
                label = { Text("Página actual") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = current.currentPageError != null,
                supportingText = current.currentPageError?.let { message -> { Text(message) } },
                enabled = !current.isBusy,
            )
            OutlinedTextField(
                value = current.percentageInput,
                onValueChange = viewModel::onPercentageChanged,
                label = { Text("Porcentaje de lectura (0–100)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = current.percentageError != null,
                supportingText = current.percentageError?.let { message -> { Text(message) } },
                enabled = !current.isBusy,
            )

            Text("Guardar estado actual", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
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
