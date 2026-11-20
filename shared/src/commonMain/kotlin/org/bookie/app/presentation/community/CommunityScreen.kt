package org.bookie.app.presentation.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.domain.model.Community
import org.bookie.app.domain.model.CommunityComment
import org.bookie.app.presentation.components.BookieEmptyState
import org.bookie.app.presentation.components.BookieErrorState
import org.bookie.app.presentation.components.BookieLoadingState

@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel,
    compatibilityViewModel: ReadingCompatibilityViewModel? = null,
    currentUserId: String? = null,
    onSignIn: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val commentThreads by viewModel.commentThreads.collectAsStateWithLifecycle()
    val create by viewModel.create.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) { viewModel.loadCommunities() }

    if (compatibilityViewModel != null) {
        ReadingCompatibilityDialog(compatibilityViewModel, onSignIn)
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (create.visible) {
            Text("Crear comunidad", style = MaterialTheme.typography.headlineSmall)
            org.bookie.app.presentation.components.BookieTextField(
                value = create.name,
                onValueChange = viewModel::onCreateNameChanged,
                label = "Nombre",
                placeholder = "Ej: Lectores de fantasía",
                enabled = !create.saving,
                modifier = Modifier.fillMaxWidth(),
            )
            androidx.compose.material3.OutlinedTextField(
                value = create.description,
                onValueChange = viewModel::onCreateDescriptionChanged,
                label = { Text("Descripción") },
                minLines = 3,
                enabled = !create.saving,
                modifier = Modifier.fillMaxWidth(),
            )
            create.error?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
            }
            if (create.signInRequired) {
                Text("Iniciá sesión para crear una comunidad.")
            }
            Button(
                onClick = viewModel::submitCreate,
                enabled = !create.saving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (create.saving) "Creando comunidad..." else "Crear comunidad") }
            TextButton(
                onClick = viewModel::closeCreate,
                enabled = !create.saving,
            ) { Text("Cancelar") }
        } else if (selected != null) {
            TextButton(onClick = viewModel::showCommunities) { Text("Volver a comunidades") }
            Text(selected!!.name, style = MaterialTheme.typography.headlineSmall)
            Text(selected!!.description)
            if (selected!!.tags.isNotEmpty()) {
                Text(selected!!.tags.joinToString(" · "), style = MaterialTheme.typography.labelMedium)
            }
            Text("Publicaciones", style = MaterialTheme.typography.titleLarge)
            when (val current = posts) {
                CommunityPostsState.Idle, CommunityPostsState.Loading ->
                    BookieLoadingState(message = "Cargando publicaciones...")
                CommunityPostsState.Empty ->
                    BookieEmptyState(message = "Todavía no hay publicaciones en esta comunidad.")
                is CommunityPostsState.Error ->
                    BookieErrorState(message = current.message, onRetry = viewModel::refreshPosts)
                is CommunityPostsState.Ready -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(current.posts, key = { it.id }) { post ->
                        val targetUserId = post.author?.id?.takeIf(String::isNotBlank)
                            ?: post.authorId.takeIf(String::isNotBlank)
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(post.title, style = MaterialTheme.typography.titleMedium)
                                post.author?.username?.let { Text("Por $it", style = MaterialTheme.typography.labelMedium) }
                                Text(post.content)
                                Text(post.createdAt.substringBefore('T'), style = MaterialTheme.typography.labelSmall)
                                if (
                                    compatibilityViewModel != null && targetUserId != null &&
                                    targetUserId != currentUserId
                                ) {
                                    TextButton(
                                        onClick = { compatibilityViewModel.compareWith(targetUserId) },
                                    ) { Text("Ver compatibilidad lectora") }
                                }
                                val commentState = commentThreads[post.id] ?: CommunityCommentThreadState()
                                TextButton(
                                    onClick = { viewModel.toggleCommentComposer(post.id) },
                                    enabled = !commentState.sending,
                                ) {
                                    Text(if (commentState.expanded) "Cerrar comentarios" else "Comentar")
                                }
                                if (commentState.expanded) {
                                    CommunityCommentComposer(
                                        state = commentState,
                                        onDraftChanged = { viewModel.onCommentDraftChanged(post.id, it) },
                                        onReply = { viewModel.replyToComment(post.id, it) },
                                        onCancelReply = { viewModel.cancelReply(post.id) },
                                        onSend = { viewModel.sendComment(post.id) },
                                        onRetryComments = { viewModel.retryComments(post.id) },
                                        onDismissFeedback = { viewModel.dismissCommentFeedback(post.id) },
                                        onSignIn = onSignIn,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Text("Comunidades", style = MaterialTheme.typography.headlineSmall)
            Button(
                onClick = viewModel::openCreate,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Crear comunidad") }
            Button(
                onClick = viewModel::loadRecommendations,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Ver recomendaciones para mí") }

            when (val current = recommendations) {
                CommunityRecommendationsState.Idle -> Unit
                CommunityRecommendationsState.Loading ->
                    BookieLoadingState(message = "Buscando comunidades para vos...")
                CommunityRecommendationsState.Empty ->
                    BookieEmptyState(message = "Todavía no hay recomendaciones.")
                CommunityRecommendationsState.SignInRequired ->
                    Text("Iniciá sesión para ver recomendaciones personalizadas.")
                is CommunityRecommendationsState.Error ->
                    BookieErrorState(message = current.message, onRetry = viewModel::loadRecommendations)
                is CommunityRecommendationsState.Ready -> {
                    Text("Recomendadas para vos", style = MaterialTheme.typography.titleMedium)
                    current.communities.take(3).forEach { community ->
                        CommunityCard(
                            community = community,
                            onClick = { viewModel.selectCommunity(community) },
                        )
                    }
                }
            }

            Text("Explorá comunidades", style = MaterialTheme.typography.titleLarge)
            when (val current = catalog) {
                CommunityCatalogState.Idle, CommunityCatalogState.Loading ->
                    BookieLoadingState(message = "Cargando comunidades...")
                CommunityCatalogState.Empty ->
                    BookieEmptyState(message = "Todavía no hay comunidades disponibles.")
                is CommunityCatalogState.Error ->
                    BookieErrorState(message = current.message, onRetry = viewModel::retryCommunities)
                is CommunityCatalogState.Ready -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(current.communities, key = Community::id) { community ->
                        CommunityCard(community) { viewModel.selectCommunity(community) }
                    }
                    item {
                        current.pageError?.let { error ->
                            Text(error, color = MaterialTheme.colorScheme.error)
                        }
                        if (current.hasMore) {
                            Button(
                                onClick = viewModel::loadMoreCommunities,
                                enabled = !current.loadingMore,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (current.loadingMore) "Cargando..." else "Ver más comunidades")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityCommentComposer(
    state: CommunityCommentThreadState,
    onDraftChanged: (String) -> Unit,
    onReply: (CommunityComment) -> Unit,
    onCancelReply: () -> Unit,
    onSend: () -> Unit,
    onRetryComments: () -> Unit,
    onDismissFeedback: () -> Unit,
    onSignIn: (() -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (state.loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        state.loadError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetryComments, enabled = !state.loading) {
                Text("Reintentar carga")
            }
        }
        if (state.loaded && !state.loading && state.comments.isEmpty()) {
            Text("Todavía no hay comentarios.", style = MaterialTheme.typography.bodySmall)
        }
        state.comments.forEach { comment ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (comment.parentId != null) {
                    Text("Respuesta", style = MaterialTheme.typography.labelSmall)
                }
                Text(comment.content)
                Text(comment.createdAt.substringBefore('T'), style = MaterialTheme.typography.labelSmall)
                TextButton(
                    onClick = { onReply(comment) },
                    enabled = !state.sending,
                ) { Text("Responder") }
            }
            HorizontalDivider()
        }
        state.replyTo?.let { parent ->
            Text("Respondiendo a: ${parent.content}", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onCancelReply, enabled = !state.sending) { Text("Cancelar respuesta") }
        }
        state.notice?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.primary)
            TextButton(onClick = onDismissFeedback) { Text("Cerrar") }
        }
        state.error?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onDismissFeedback) { Text("Cerrar") }
        }
        if (state.signInRequired) {
            if (onSignIn != null) {
                Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
                    Text("Iniciar sesión")
                }
            } else {
                Text("Iniciá sesión para comentar.")
            }
        }
        OutlinedTextField(
            value = state.draft,
            onValueChange = { if (it.length <= 5000) onDraftChanged(it) },
            label = { Text(if (state.replyTo == null) "Comentario" else "Respuesta") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            enabled = !state.sending,
        )
        Button(
            onClick = onSend,
            enabled = !state.sending && state.draft.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.sending) "Enviando..." else "Enviar")
        }
    }
}

@Composable
private fun ReadingCompatibilityDialog(
    viewModel: ReadingCompatibilityViewModel,
    onSignIn: (() -> Unit)?,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state == ReadingCompatibilityUiState.Idle) return

    AlertDialog(
        onDismissRequest = viewModel::dismiss,
        title = { Text("Compatibilidad lectora") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (val current = state) {
                    ReadingCompatibilityUiState.Idle -> Unit
                    is ReadingCompatibilityUiState.Loading -> CircularProgressIndicator()
                    is ReadingCompatibilityUiState.Error -> {
                        Text(current.message, color = MaterialTheme.colorScheme.error)
                    }
                    is ReadingCompatibilityUiState.Ready -> {
                        val compatibility = current.compatibility
                        Text(compatibility.user.username ?: "Lector/a")
                        Text("${compatibility.compatibilityPercentage.coerceIn(0, 100)}% de compatibilidad")
                        Text(
                            when (compatibility.dataQuality.lowercase()) {
                                "limited" -> "Datos limitados"
                                "good" -> "Datos suficientes"
                                else -> "Calidad de datos no disponible"
                            },
                            style = MaterialTheme.typography.labelSmall,
                        )
                        compatibility.reasons.forEach { reason -> Text("• $reason") }
                        if (compatibility.sharedGenres.isNotEmpty()) {
                            Text("Géneros en común: ${compatibility.sharedGenres.take(3).joinToString()}")
                        }
                        if (compatibility.sharedAuthors.isNotEmpty()) {
                            Text("Autores en común: ${compatibility.sharedAuthors.take(3).joinToString()}")
                        }
                        compatibility.sharedBooks.take(3).forEach { book ->
                            Text("Libro en común: ${book.title}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::dismiss) { Text("Cerrar") }
        },
        dismissButton = {
            val error = state as? ReadingCompatibilityUiState.Error
            if (error?.sessionExpired == true && onSignIn != null) {
                TextButton(onClick = {
                    viewModel.dismiss()
                    onSignIn()
                }) { Text("Iniciar sesión") }
            } else if (error != null) {
                TextButton(onClick = viewModel::retry) { Text("Reintentar") }
            }
        },
    )
}

@Composable
private fun CommunityCard(
    community: Community,
    onClick: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(community.name, style = MaterialTheme.typography.titleMedium)
            Text(community.description, style = MaterialTheme.typography.bodyMedium)
            if (community.tags.isNotEmpty()) {
                Text(community.tags.joinToString(" · "), style = MaterialTheme.typography.labelSmall)
            }
            community.reason?.let {
                Text(it, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
