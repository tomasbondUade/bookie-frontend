package org.bookie.app.presentation.tinder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.domain.model.RecommendationSource
import org.bookie.app.domain.model.SwipeType
import org.bookie.app.presentation.components.BookieEmptyState
import org.bookie.app.presentation.components.BookieErrorState
import org.bookie.app.presentation.components.BookieLoadingState

@Composable
fun BookTinderScreen(
    viewModel: BookTinderViewModel,
    modifier: Modifier = Modifier,
    onSignIn: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        onBack?.let { goBack ->
            TextButton(onClick = goBack) { Text("Volver") }
        }
        Text("Tinder de libros", style = MaterialTheme.typography.headlineSmall)

        when (val current = state) {
            BookTinderUiState.Loading -> BookieLoadingState(
                modifier = Modifier.weight(1f),
                message = "Buscando libros para vos...",
            )
            is BookTinderUiState.Error -> BookieErrorState(
                modifier = Modifier.weight(1f),
                message = current.message,
                onRetry = viewModel::loadFeed,
            )
            is BookTinderUiState.SignInRequired -> {
                BookieErrorState(
                    modifier = Modifier.weight(1f),
                    message = "Tu sesión venció. Iniciá sesión para seguir descubriendo libros.",
                )
                onSignIn?.let { signIn ->
                    Button(onClick = signIn, modifier = Modifier.fillMaxWidth()) {
                        Text("Iniciar sesión")
                    }
                }
            }
            is BookTinderUiState.Empty -> Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BookieEmptyState(
                    message = "No hay libros nuevos para mostrar por ahora.",
                )
                TextButton(onClick = viewModel::loadFeed) { Text("Actualizar") }
            }
            is BookTinderUiState.Exhausted -> Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BookieEmptyState(message = "Llegaste al final de esta selección.")
                TextButton(onClick = viewModel::loadFeed) { Text("Buscar más libros") }
            }
            is BookTinderUiState.Ready -> {
                val recommendation = current.feed.books[current.currentIndex]
                Text(
                    text = if (current.feed.source == RecommendationSource.AI) {
                        "Recomendado según tus gustos"
                    } else {
                        "Una lectura para descubrir"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Libro ${current.currentIndex + 1} de ${current.feed.count}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = recommendation.book.title,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = recommendation.book.authors.joinToString().ifBlank {
                                "Autor desconocido"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        recommendation.book.genres.takeIf { it.isNotEmpty() }?.let { genres ->
                            Text(genres.joinToString(" · "), style = MaterialTheme.typography.labelMedium)
                        }
                        recommendation.book.description?.takeIf(String::isNotBlank)?.let { description ->
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        recommendation.reasons.firstOrNull()?.let { reason ->
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                current.actionError?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { viewModel.swipe(SwipeType.DISLIKE) },
                        enabled = !current.submitting,
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbDown,
                            contentDescription = "No me interesa",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (current.submitting) {
                        CircularProgressIndicator(modifier = Modifier.padding(horizontal = 24.dp))
                    }
                    IconButton(
                        onClick = { viewModel.swipe(SwipeType.LIKE) },
                        enabled = !current.submitting,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Me interesa",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
