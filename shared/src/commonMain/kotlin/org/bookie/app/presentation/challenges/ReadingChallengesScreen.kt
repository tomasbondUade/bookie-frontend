package org.bookie.app.presentation.challenges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.domain.model.ReadingChallenge
import org.bookie.app.domain.model.ReadingChallengeProgress
import org.bookie.app.presentation.components.BookieEmptyState
import org.bookie.app.presentation.components.BookieErrorState
import org.bookie.app.presentation.components.BookieLoadingState

@Composable
fun ReadingChallengesScreen(
    viewModel: ReadingChallengesViewModel,
    onSignIn: (() -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.load() }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBack) { Text("Volver") }
            Text("Retos de lectura", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            if (state !is ReadingChallengesUiState.Loading) {
                TextButton(onClick = viewModel::retry) { Text("Actualizar") }
            }
        }

        when (val current = state) {
            ReadingChallengesUiState.Idle, ReadingChallengesUiState.Loading ->
                BookieLoadingState(message = "Cargando retos...")

            ReadingChallengesUiState.Empty -> {
                BookieEmptyState(message = "Todavía no hay retos disponibles.")
                TextButton(onClick = viewModel::retry) { Text("Actualizar") }
            }

            is ReadingChallengesUiState.Error -> {
                BookieErrorState(message = current.message, onRetry = viewModel::retry)
                if (current.sessionExpired) {
                    onSignIn?.let { signIn ->
                        Button(onClick = signIn, modifier = Modifier.fillMaxWidth()) {
                            Text("Iniciar sesión")
                        }
                    }
                }
            }

            is ReadingChallengesUiState.Ready -> {
                current.message?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = viewModel::dismissMessage) { Text("Cerrar") }
                }
                current.actionError?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error)
                    if (current.sessionExpired) {
                        onSignIn?.let { signIn ->
                            Button(onClick = signIn, modifier = Modifier.fillMaxWidth()) {
                                Text("Iniciar sesión")
                            }
                        }
                    } else {
                        TextButton(onClick = viewModel::dismissMessage) { Text("Cerrar") }
                    }
                }
                if (current.refreshing) CircularProgressIndicator()
                val progressByChallenge = current.progress.associateBy(ReadingChallengeProgress::challengeId)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(current.challenges, key = ReadingChallenge::id) { challenge ->
                        ReadingChallengeCard(
                            challenge = challenge,
                            progress = progressByChallenge[challenge.id],
                            joining = current.joiningChallengeId == challenge.id,
                            anyJoinInProgress = current.joiningChallengeId != null,
                            onJoin = { viewModel.join(challenge.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingChallengeCard(
    challenge: ReadingChallenge,
    progress: ReadingChallengeProgress?,
    joining: Boolean,
    anyJoinInProgress: Boolean,
    onJoin: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(challenge.title, style = MaterialTheme.typography.titleMedium)
            challenge.description?.takeIf(String::isNotBlank)?.let { Text(it) }
            Text(challenge.targetDescription(), style = MaterialTheme.typography.bodyMedium)
            if (challenge.type == "GENRE") {
                challenge.genre?.let { Text("Género: $it", style = MaterialTheme.typography.bodySmall) }
            }
            if (challenge.joined) {
                Text("Ya participás", color = MaterialTheme.colorScheme.primary)
                progress?.let {
                    Text("Progreso: ${it.completed} de ${it.target} · ${it.percentage.coerceIn(0.0, 100.0).toInt()}%")
                }
            } else {
                Button(
                    onClick = onJoin,
                    enabled = !anyJoinInProgress,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (joining) "Uniéndote..." else "Unirme al reto")
                }
            }
        }
    }
}

private fun ReadingChallenge.targetDescription(): String = when (type) {
    "BOOK_COUNT" -> "Objetivo: leer $target libros"
    "GENRE" -> "Objetivo: leer $target libros del género indicado"
    else -> "Objetivo: $target"
}
