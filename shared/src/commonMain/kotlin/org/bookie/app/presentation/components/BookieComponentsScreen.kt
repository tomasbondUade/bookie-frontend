package org.bookie.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BookieComponentsScreen(
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Componentes de Bookie",
            style = MaterialTheme.typography.headlineMedium
        )

        // Botones
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Botones", style = MaterialTheme.typography.titleLarge)

            BookieButton(
                text = "Botón principal",
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            )

            BookieButton(
                text = "Botón secundario",
                onClick = {},
                type = BookieButtonType.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            )

            BookieButton(
                text = "Botón de texto",
                onClick = {},
                type = BookieButtonType.TEXT,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Campos de texto
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Campos de texto", style = MaterialTheme.typography.titleLarge)

            BookieTextField(
                value = name,
                onValueChange = { name = it },
                label = "Nombre",
                placeholder = "Ingresá tu nombre"
            )

            BookieTextField(
                value = search,
                onValueChange = { search = it },
                label = "Buscar libro",
                isError = true,
                errorMessage = "Este campo contiene un error"
            )
        }

        // Cards de libros
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cards de libros", style = MaterialTheme.typography.titleLarge)

            BookieBookCard(
                title = "Cien años de soledad",
                author = "Gabriel García Márquez",
                rating = "4.8"
            )

            BookieBookCard(
                title = "Orgullo y prejuicio",
                author = "Jane Austen"
            )
        }

        // Estados
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Estados de pantalla", style = MaterialTheme.typography.titleLarge)

            BookieLoadingState(message = "Cargando libros...")

            BookieEmptyState(
                message = "Todavía no hay libros para mostrar"
            )

            BookieErrorState(
                message = "No se pudieron cargar los libros",
                onRetry = {}
            )
        }
    }
}