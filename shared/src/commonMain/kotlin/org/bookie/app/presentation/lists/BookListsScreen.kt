package org.bookie.app.presentation.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.SecondaryTabRow
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.domain.model.Book
import org.bookie.app.domain.model.BookList
import org.bookie.app.presentation.library.LibraryExperienceScreen
import org.bookie.app.presentation.library.LibraryExperienceViewModel

@Composable
fun LibraryWorkspaceScreen(
    libraryViewModel: LibraryExperienceViewModel,
    listsViewModel: BookListsViewModel,
    onSignIn: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    Column(modifier.fillMaxSize()) {
        SecondaryTabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Libros") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Mis listas") })
        }
        when (selectedTab) {
            0 -> LibraryExperienceScreen(libraryViewModel, Modifier.weight(1f))
            else -> BookListsScreen(listsViewModel, onSignIn, Modifier.weight(1f))
        }
    }
}

@Composable
fun BookListsScreen(
    viewModel: BookListsViewModel,
    onSignIn: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var createDialog by remember { mutableStateOf(false) }
    var editDialog by remember { mutableStateOf(false) }
    var addBookDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        if (viewModel.uiState.value.content == BookListsContentState.Idle) viewModel.load()
    }
    LaunchedEffect(state.action, createDialog, editDialog, addBookDialog) {
        val action = state.action
        if (action is BookListActionState.Succeeded) {
            when (action.message) {
                "Lista creada." -> if (createDialog) createDialog = false
                "Lista actualizada." -> if (editDialog) editDialog = false
                "Libro agregado a la lista." -> if (addBookDialog) addBookDialog = false
            }
        }
    }

    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(state.selectedList?.name ?: "Mis listas", style = MaterialTheme.typography.headlineSmall)
            if (state.selectedList == null) {
                Button(onClick = { viewModel.dismissAction(); createDialog = true }) { Text("Nueva lista") }
            } else {
                TextButton(onClick = viewModel::closeList) { Text("Volver") }
            }
        }
        if (!createDialog && !editDialog && !addBookDialog) {
            ListActionMessage(state.action, viewModel::dismissAction, onSignIn)
        }

        val selected = state.selectedList
        if (selected != null) {
            BookListDetail(
                list = selected,
                onEdit = { viewModel.dismissAction(); editDialog = true },
                onDelete = { viewModel.requestDelete(selected.id) },
                onAddBook = { viewModel.dismissAction(); addBookDialog = true },
                onRemoveBook = viewModel::requestRemoveBook,
            )
        } else {
            when (val content = state.content) {
                BookListsContentState.Idle,
                is BookListsContentState.Loading -> {
                    if (content is BookListsContentState.Loading && content.previous != null) {
                        BookListsItems(content.previous.lists, viewModel)
                    } else {
                        CircularProgressIndicator()
                        Text("Cargando tus listas")
                    }
                }
                is BookListsContentState.Failed -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(content.message, color = MaterialTheme.colorScheme.error)
                    if (content.signInRequired) {
                        onSignIn?.let { Button(onClick = it) { Text("Iniciar sesión") } }
                    } else {
                        Button(onClick = { viewModel.load() }) { Text("Reintentar") }
                    }
                }
                is BookListsContentState.Ready -> {
                    if (content.snapshot.lists.isEmpty()) {
                        Text("Todavía no creaste listas.")
                    } else {
                        BookListsItems(content.snapshot.lists, viewModel)
                    }
                    if (content.snapshot.page < content.snapshot.totalPages) {
                        OutlinedButton(
                            onClick = viewModel::loadNextPage,
                            enabled = !content.snapshot.loadingMore,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (content.snapshot.loadingMore) CircularProgressIndicator()
                            else Text("Cargar más")
                        }
                    }
                }
            }
        }
    }

    if (createDialog) {
        BookListEditorDialog(
            title = "Nueva lista",
            action = state.action,
            onSignIn = onSignIn,
            onDismiss = { createDialog = false },
            onSave = { name, description, isPublic ->
                viewModel.create(name, description, isPublic)
            },
        )
    }
    if (editDialog && state.selectedList != null) {
        BookListEditorDialog(
            title = "Editar lista",
            initialList = state.selectedList,
            onDismiss = { editDialog = false },
            onSave = viewModel::updateSelected,
            action = state.action,
            onSignIn = onSignIn,
        )
    }
    if (addBookDialog && state.selectedList != null) {
        AddBookDialog(
            searchState = state.bookSearch,
            action = state.action,
            onSearch = viewModel::searchBooks,
            onAdd = viewModel::addBook,
            onDismiss = { addBookDialog = false },
            onSignIn = onSignIn,
        )
    }

    state.pendingDeleteList?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Eliminar lista") },
            text = { Text("¿Querés eliminar «${pending.name}» y todos los libros de esta lista?") },
            confirmButton = { Button(onClick = viewModel::confirmDelete) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = viewModel::cancelDelete) { Text("Cancelar") } },
        )
    }
    state.pendingRemoveBookId?.let { bookId ->
        val bookName = state.selectedList?.items.orEmpty()
            .firstOrNull { it.bookId == bookId }?.book?.title ?: "este libro"
        AlertDialog(
            onDismissRequest = viewModel::cancelRemoveBook,
            title = { Text("Quitar libro") },
            text = { Text("¿Querés quitar «$bookName» de la lista?") },
            confirmButton = { Button(onClick = viewModel::confirmRemoveBook) { Text("Quitar") } },
            dismissButton = { TextButton(onClick = viewModel::cancelRemoveBook) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun BookListsItems(lists: List<BookList>, viewModel: BookListsViewModel) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(lists, key = BookList::id) { list ->
            Card(onClick = { viewModel.selectList(list.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(list.name, style = MaterialTheme.typography.titleMedium)
                    list.description?.takeIf(String::isNotBlank)?.let { Text(it) }
                    Text(if (list.isPublic) "Pública" else "Privada", style = MaterialTheme.typography.bodySmall)
                    Text("${list.items?.size ?: 0} libros", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun BookListDetail(
    list: BookList,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddBook: () -> Unit,
    onRemoveBook: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        list.description?.takeIf(String::isNotBlank)?.let { Text(it) }
        Text(if (list.isPublic) "Pública" else "Privada")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onEdit) { Text("Editar") }
            OutlinedButton(onClick = onDelete) { Text("Eliminar") }
            Button(onClick = onAddBook) { Text("Agregar libro") }
        }
        val books = list.items.orEmpty()
        if (books.isEmpty()) {
            Text("Esta lista todavía no tiene libros.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(books, key = { it.id }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(item.book?.title ?: "Libro del catálogo", style = MaterialTheme.typography.titleSmall)
                                Text(item.book?.authors?.joinToString().orEmpty())
                            }
                            TextButton(onClick = { onRemoveBook(item.bookId) }) { Text("Quitar") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookListEditorDialog(
    title: String,
    initialList: BookList? = null,
    action: BookListActionState = BookListActionState.Idle,
    onSignIn: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean) -> Unit,
) {
    var name by remember(initialList?.id) { mutableStateOf(initialList?.name.orEmpty()) }
    var description by remember(initialList?.id) { mutableStateOf(initialList?.description.orEmpty()) }
    var isPublic by remember(initialList?.id) { mutableStateOf(initialList?.isPublic ?: false) }
    var error by remember(initialList?.id) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPublic, onCheckedChange = { isPublic = it })
                    Text("Lista pública")
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (action is BookListActionState.Failed) {
                    Text(action.message, color = MaterialTheme.colorScheme.error)
                    if (action.signInRequired) {
                        onSignIn?.let { Button(onClick = it) { Text("Iniciar sesión") } }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) error = "El nombre de la lista es obligatorio."
                    else onSave(name, description, isPublic)
                },
                enabled = action !is BookListActionState.Working,
            ) {
                if (action is BookListActionState.Working) CircularProgressIndicator()
                else Text("Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun AddBookDialog(
    searchState: BookListSearchState,
    action: BookListActionState,
    onSearch: (String) -> Unit,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
    onSignIn: (() -> Unit)? = null,
) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar un libro") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Título o autor") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = { onSearch(query) }, enabled = query.isNotBlank()) { Text("Buscar") }
                when (searchState) {
                    BookListSearchState.Idle -> Text("Buscá por título o autor.")
                    BookListSearchState.Loading -> CircularProgressIndicator()
                    is BookListSearchState.Empty -> Text("No se encontraron libros para «${searchState.query}».")
                    is BookListSearchState.Failed -> Text(searchState.message, color = MaterialTheme.colorScheme.error)
                    is BookListSearchState.Results -> searchState.books.forEach { book ->
                        BookSearchResult(book, enabled = action !is BookListActionState.Working) {
                            onAdd(book.id)
                        }
                    }
                }
                if (action is BookListActionState.Working) Text(action.message)
                if (action is BookListActionState.Failed) {
                    Text(action.message, color = MaterialTheme.colorScheme.error)
                    if (action.signInRequired) {
                        onSignIn?.let { Button(onClick = it) { Text("Iniciar sesión") } }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}

@Composable
private fun BookSearchResult(book: Book, enabled: Boolean, onAdd: () -> Unit) {
    Card(onClick = onAdd, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(book.title, style = MaterialTheme.typography.titleSmall)
            Text(book.authors.joinToString().ifBlank { "Autor desconocido" })
        }
    }
}

@Composable
private fun ListActionMessage(
    action: BookListActionState,
    onDismiss: () -> Unit,
    onSignIn: (() -> Unit)?,
) {
    when (action) {
        BookListActionState.Idle -> Unit
        is BookListActionState.Working -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator()
            Text(action.message, Modifier.padding(start = 8.dp))
        }
        is BookListActionState.Succeeded -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(action.message, color = MaterialTheme.colorScheme.primary)
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
        is BookListActionState.Failed -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(action.message, color = MaterialTheme.colorScheme.error)
            if (action.signInRequired) onSignIn?.let { TextButton(onClick = it) { Text("Iniciar sesión") } }
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    }
}
