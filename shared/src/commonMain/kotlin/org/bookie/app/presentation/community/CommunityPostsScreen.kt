package org.bookie.app.presentation.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.domain.model.CommunityPost

@Composable
fun CommunityPostsScreen(viewModel: CommunityPostsViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.load() }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Publicaciones", style = MaterialTheme.typography.headlineSmall)
        val selected = state.selectedPost
        if (selected != null) {
            TextButton(onClick = { viewModel.selectPost(null) }) { Text("Volver al feed") }
            Text(selected.title, style = MaterialTheme.typography.titleLarge)
            Text(selected.author?.username ?: "Autor")
            Text(selected.content)
            Text(selected.createdAt, style = MaterialTheme.typography.bodySmall)
        } else {
            Text("Nueva publicación", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = state.title, onValueChange = viewModel::onTitleChanged,
                label = { Text("Título") }, modifier = Modifier.fillMaxWidth(),
                enabled = !state.publishing,
            )
            OutlinedTextField(
                value = state.content, onValueChange = viewModel::onContentChanged,
                label = { Text("Contenido") }, modifier = Modifier.fillMaxWidth(),
                minLines = 3, enabled = !state.publishing,
            )
            Button(
                onClick = viewModel::publish, enabled = !state.publishing,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (state.publishing) "Publicando…" else "Publicar") }
            state.publishError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.publishSuccess) Text("Publicación confirmada por el servidor.")
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Feed", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = viewModel::load, enabled = !state.loading) { Text("Actualizar") }
            }
            if (state.loading) CircularProgressIndicator()
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = viewModel::load) { Text("Reintentar") }
            }
            if (!state.loading && state.error == null && state.posts.isEmpty()) {
                Text("Todavía no hay publicaciones.")
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.posts, key = CommunityPost::id) { post ->
                    Card(onClick = { viewModel.selectPost(post) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(post.title, style = MaterialTheme.typography.titleMedium)
                            Text(post.author?.username ?: "Autor", style = MaterialTheme.typography.bodySmall)
                            Text(post.content, maxLines = 3)
                        }
                    }
                }
            }
        }
    }
}
