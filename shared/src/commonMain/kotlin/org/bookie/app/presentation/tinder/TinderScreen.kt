package org.bookie.app.presentation.tinder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TinderScreen(viewModel: TinderViewModel, onSignIn: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier = modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (val current = state) {
            TinderUiState.Loading -> CircularProgressIndicator()
            TinderUiState.Finished -> {
                Text("No hay más libros por ahora")
                Button(onClick = viewModel::load) { Text("Buscar más") }
            }
            is TinderUiState.Error -> {
                Text(current.message, color = MaterialTheme.colorScheme.error)
                if (current.unauthorized && onSignIn != null) {
                    Button(onClick = onSignIn) { Text("Iniciar sesión") }
                } else {
                    Button(onClick = viewModel::load) { Text("Reintentar") }
                }
            }
            is TinderUiState.ShowingBook -> {
                Text(current.book.title, style = MaterialTheme.typography.headlineSmall)
                Text(current.book.authors.joinToString())
                Text(current.book.description.orEmpty())
                current.reasons.forEach { Text(it) }
                Text("${current.position} de ${current.total}")
                current.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(enabled = !current.sending, onClick = viewModel::dislike) { Text("DISLIKE") }
                    Button(enabled = !current.sending, onClick = viewModel::like) { Text("LIKE") }
                }
                if (current.sending) CircularProgressIndicator()
            }
        }
    }
}
