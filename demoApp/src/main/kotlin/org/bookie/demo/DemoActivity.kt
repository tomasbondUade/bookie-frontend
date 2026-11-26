package org.bookie.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.first
import org.bookie.app.App
import org.bookie.app.domain.model.LibraryStateInput
import org.bookie.app.presentation.search.BookSearchScreen
import org.bookie.app.presentation.search.SearchUiState
import org.bookie.app.theme.BookieTheme

class DemoHostViewModel : ViewModel() {
    var runtime by mutableStateOf(DemoRuntime())
        private set
    fun reset() { runtime.close(); runtime = DemoRuntime() }
    override fun onCleared() { runtime.close() }
}

class DemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.rgb(7, 11, 32)))
        super.onCreate(savedInstanceState)
        val host = ViewModelProvider(this)[DemoHostViewModel::class.java]
        setContent { DemoContent(host) }
    }
}

@Composable
private fun DemoContent(host: DemoHostViewModel) {
    var showHelp by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val runtime = host.runtime
    val features by runtime.features.collectAsStateWithLifecycle()
    val overlayBook by runtime.overlayBook.collectAsStateWithLifecycle()
    BookieTheme {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("BOOKIE DEMO · sin conexión", Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = { showHelp = true }) { Text("Acceso") }
                TextButton(onClick = { confirmReset = true }) { Text("Reiniciar") }
            }
            Box(Modifier.weight(1f)) { key(runtime) { App(features) } }
        }
        if (showHelp) AlertDialog(onDismissRequest = { showHelp = false },
            title = { Text("Probá los flujos de Bookie") },
            text = { Text("Correo: $DEMO_EMAIL\nContraseña: $DEMO_PASSWORD\n\nTambién podés registrar una cuenta de prueba local. " +
                "Buscá «todos» para recorrer el catálogo completo.\n\nLos cambios son temporales: se mantienen al navegar y rotar el celular. " +
                "Reiniciar o abrir la app después de cerrarla por completo vuelve a los datos iniciales. No se envían correos ni datos a un servidor. " +
                "Las portadas vacías son marcadores de prueba.", Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Entendido") } })
        if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false },
            title = { Text("¿Volver a los datos iniciales?") },
            text = { Text("Se borrarán los cambios locales de esta demostración y se cerrará la sesión de prueba.") },
            confirmButton = { TextButton(onClick = { confirmReset = false; host.reset() }) { Text("Reiniciar demo") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar") } })

        // The series screen's injected open-book action uses the real catalog detail screen.
        overlayBook?.let { bookId ->
            LaunchedEffect(runtime, bookId) {
                val book = runtime.data.books.firstOrNull { it.id == bookId } ?: return@LaunchedEffect
                runtime.overlaySearch.onQueryChanged(book.title)
                runtime.overlaySearch.search()
                runtime.overlaySearch.uiState.first { it is SearchUiState.Success }
                runtime.overlaySearch.selectBook(book)
            }
            Dialog(onDismissRequest = runtime::closeBook, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                        TextButton(onClick = runtime::closeBook) { Text("Volver a la saga") }
                        val library = requireNotNull(features.libraryExperience)
                        val libraryState by library.uiState.collectAsStateWithLifecycle()
                        BookSearchScreen(runtime.overlaySearch, Modifier.weight(1f), features.bookReviews,
                            onAddToLibrary = { library.addCatalogBook(it.id) },
                            onRateBook = { book, rating -> library.save(LibraryStateInput(book.id, rating = rating), "Calificación guardada en la demo.") },
                            libraryAction = libraryState.action)
                    }
                }
            }
        }
    }
}
