package org.bookie.app.presentation.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import org.bookie.app.presentation.components.BookieActionButton as Button
import org.bookie.app.presentation.components.BookiePanelCard as Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import org.bookie.app.presentation.components.BookieInputShape
import org.bookie.app.presentation.components.bookieInputColors
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.bookie.app.presentation.components.BookiePageHeading
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.domain.model.Community
import org.bookie.app.domain.model.CommunityComment
import org.bookie.app.presentation.components.BookieEmptyState
import org.bookie.app.presentation.components.BookieErrorState
import org.bookie.app.presentation.components.BookieLoadingState
import org.bookie.app.SystemBackHandler

@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel,
    compatibilityViewModel: ReadingCompatibilityViewModel? = null,
    postsViewModelFactory: ((String) -> CommunityPostsViewModel)? = null,
    currentUserId: String? = null,
    onSignIn: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val commentThreads by viewModel.commentThreads.collectAsStateWithLifecycle()
    val create by viewModel.create.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var composePost by rememberSaveable { mutableStateOf(false) }
    var selectedPostId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedPost = (posts as? CommunityPostsState.Ready)?.posts
        ?.firstOrNull { it.id == selectedPostId }
    SystemBackHandler(enabled = selectedPostId != null) {
        selectedPostId = null
    }
    val postsViewModel = selected?.id?.let { communityId ->
        remember(communityId, postsViewModelFactory) { postsViewModelFactory?.invoke(communityId) }
    }
    val postDraft = postsViewModel?.uiState?.collectAsStateWithLifecycle()?.value

    LaunchedEffect(postDraft?.publishSuccess) {
        if (postDraft?.publishSuccess == true) {
            composePost = false
            viewModel.refreshPosts()
        }
    }

    LaunchedEffect(viewModel) { viewModel.loadCommunities() }
    LaunchedEffect(selectedPostId) {
        val postId = selectedPostId
        if (postId != null && commentThreads[postId]?.expanded != true) {
            viewModel.toggleCommentComposer(postId)
        }
    }

    if (compatibilityViewModel != null) {
        ReadingCompatibilityDialog(compatibilityViewModel, onSignIn)
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (create.visible) {
            BookiePageHeading("Creá tu comunidad", "Un nuevo lugar para compartir historias.")
            org.bookie.app.presentation.components.BookieTextField(
                value = create.name,
                onValueChange = viewModel::onCreateNameChanged,
                label = "Nombre",
                placeholder = "Ej: Lectores de fantasía",
                enabled = !create.saving,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                shape = BookieInputShape,
                colors = bookieInputColors(),
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
            if (selectedPost != null) {
                TextButton(onClick = { selectedPostId = null }) { Text("Volver a comunidad") }
                Text("POST EN ${selected!!.name.uppercase()}", style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(selectedPost.title, style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text("Por ${selectedPost.author?.username ?: "un lector"} · ${selectedPost.createdAt.substringBefore('T')}")
                    Text(selectedPost.content)
                    val targetUserId = selectedPost.author?.id?.takeIf(String::isNotBlank)
                        ?: selectedPost.authorId.takeIf(String::isNotBlank)
                    if (compatibilityViewModel != null && targetUserId != null && targetUserId != currentUserId) {
                        TextButton(onClick = { compatibilityViewModel.compareWith(targetUserId) }) {
                            Text("Ver compatibilidad lectora")
                        }
                    }
                    HorizontalDivider()
                    Text("Respuestas", style = MaterialTheme.typography.titleMedium)
                    CommunityCommentComposer(
                        state = commentThreads[selectedPost.id] ?: CommunityCommentThreadState(),
                        onDraftChanged = { viewModel.onCommentDraftChanged(selectedPost.id, it) },
                        onReply = { viewModel.replyToComment(selectedPost.id, it) },
                        onCancelReply = { viewModel.cancelReply(selectedPost.id) },
                        onSend = { viewModel.sendComment(selectedPost.id) },
                        onRetryComments = { viewModel.retryComments(selectedPost.id) },
                        onDismissFeedback = { viewModel.dismissCommentFeedback(selectedPost.id) },
                        onSignIn = onSignIn,
                    )
                }
            } else {
            TextButton(onClick = { selectedPostId = null; viewModel.showCommunities() }) {
                Text("Volver a comunidades")
            }
            BookiePageHeading(selected!!.name)
            Text(selected!!.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (selected!!.tags.isNotEmpty()) {
                Text(selected!!.tags.joinToString(" · "), style = MaterialTheme.typography.labelMedium)
            }
            if (postsViewModel != null) Button(
                onClick = {
                    if (currentUserId == null) onSignIn?.invoke() else composePost = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Crear publicación") }
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
                        Card(onClick = { selectedPostId = post.id }, modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(post.title, style = MaterialTheme.typography.titleMedium)
                                post.author?.username?.let { Text("Por $it", style = MaterialTheme.typography.labelMedium) }
                                Text(post.content)
                                Text(post.createdAt.substringBefore('T'), style = MaterialTheme.typography.labelSmall)
                                Text("Abrir post", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item { BookiePageHeading("Comunidad", "Las buenas historias se comparten.") }
                item {
                    OutlinedTextField(shape = BookieInputShape, colors = bookieInputColors(), value = query, onValueChange = { query = it },
                        label = { Text("Buscar comunidad") }, placeholder = { Text("Fantasía, misterio...") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                item {
                    Button(onClick = viewModel::openCreate, modifier = Modifier.fillMaxWidth()) { Text("Crear comunidad") }
                    TextButton(onClick = viewModel::loadRecommendations, modifier = Modifier.fillMaxWidth()) {
                        Text("Encontrar comunidades para mí")
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when (val current = recommendations) {
                            CommunityRecommendationsState.Idle -> Unit
                            CommunityRecommendationsState.Loading -> BookieLoadingState(message = "Buscando comunidades para vos...")
                            CommunityRecommendationsState.Empty -> BookieEmptyState(message = "Todavía no hay recomendaciones.")
                            CommunityRecommendationsState.SignInRequired -> {
                                Text("Iniciá sesión para ver recomendaciones personalizadas.")
                                onSignIn?.let { TextButton(onClick = it) { Text("Iniciar sesión") } }
                            }
                            is CommunityRecommendationsState.Error -> BookieErrorState(message = current.message, onRetry = viewModel::loadRecommendations)
                            is CommunityRecommendationsState.Ready -> {
                                Text("Para vos", style = MaterialTheme.typography.titleLarge)
                                current.communities.take(3).forEach { community ->
                                    CommunityCard(community) { viewModel.selectCommunity(community) }
                                }
                            }
                        }
                    }
                }
                item { Text("Descubrí tu círculo lector", style = MaterialTheme.typography.titleMedium) }
                when (val current = catalog) {
                    CommunityCatalogState.Idle, CommunityCatalogState.Loading -> item { BookieLoadingState(message = "Cargando comunidades...") }
                    CommunityCatalogState.Empty -> item { BookieEmptyState(message = "Todavía no hay comunidades disponibles.") }
                    is CommunityCatalogState.Error -> item { BookieErrorState(message = current.message, onRetry = viewModel::retryCommunities) }
                    is CommunityCatalogState.Ready -> {
                        val visible = current.communities.filter { community ->
                            query.isBlank() || community.name.contains(query.trim(), ignoreCase = true) ||
                                community.tags.any { it.contains(query.trim(), ignoreCase = true) }
                        }
                        if (visible.isEmpty()) item { BookieEmptyState(message = "No hay coincidencias en las comunidades cargadas.") }
                        items(visible, key = Community::id) { community ->
                            CommunityCard(community) { viewModel.selectCommunity(community) }
                        }
                        item {
                            current.pageError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            if (current.hasMore) Button(onClick = viewModel::loadMoreCommunities,
                                enabled = !current.loadingMore, modifier = Modifier.fillMaxWidth()) {
                                Text(if (current.loadingMore) "Cargando..." else "Ver más comunidades")
                            }
                        }
                    }
                }
            }
        }
    }

    if (composePost && postsViewModel != null && postDraft != null) {
        AlertDialog(
            onDismissRequest = { if (!postDraft.publishing) composePost = false },
            title = { Text("Crear publicación") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        shape = BookieInputShape,
                        colors = bookieInputColors(),
                        value = postDraft.title,
                        onValueChange = postsViewModel::onTitleChanged,
                        label = { Text("Título") },
                        enabled = !postDraft.publishing,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        shape = BookieInputShape,
                        colors = bookieInputColors(),
                        value = postDraft.content,
                        onValueChange = postsViewModel::onContentChanged,
                        label = { Text("Contenido") },
                        minLines = 3,
                        enabled = !postDraft.publishing,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    postDraft.publishError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                Button(
                    onClick = postsViewModel::publish,
                    enabled = !postDraft.publishing,
                ) { Text(if (postDraft.publishing) "Publicando" else "Publicar") }
            },
            dismissButton = {
                TextButton(onClick = { composePost = false }, enabled = !postDraft.publishing) {
                    Text("Cancelar")
                }
            },
        )
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
            shape = BookieInputShape,
            colors = bookieInputColors(),
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                org.bookie.app.presentation.components.BookieAvatar(community.name)
                Column(Modifier.weight(1f)) {
                    Text(community.name, style = MaterialTheme.typography.titleMedium)
                    Text("Un espacio para conectar", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
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
