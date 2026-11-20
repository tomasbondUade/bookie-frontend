package org.bookie.app.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.presentation.auth.AuthUiState
import org.bookie.app.presentation.auth.AuthViewModel

/** Displays only real Supabase session fields, never fabricated profile data. */
@Composable
fun AccountScreen(
    authViewModel: AuthViewModel,
    onOpenStatistics: (() -> Unit)? = null,
    onOpenPreferences: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val state by authViewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Mi cuenta", style = MaterialTheme.typography.headlineSmall)
        when (val current = state) {
            is AuthUiState.SignedIn -> {
                current.username?.takeIf(String::isNotBlank)?.let {
                    Text("Usuario: $it")
                }
                current.email?.takeIf(String::isNotBlank)?.let {
                    Text("Correo: $it")
                }
                current.syncWarning?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                current.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                onOpenPreferences?.let { action ->
                    Button(onClick = action, modifier = Modifier.fillMaxWidth()) {
                        Text("Preferencias de lectura")
                    }
                }
                onOpenStatistics?.let { action ->
                    Button(onClick = action, modifier = Modifier.fillMaxWidth()) {
                        Text("Estadísticas de lectura")
                    }
                }
                Button(onClick = authViewModel::signOut, modifier = Modifier.fillMaxWidth()) {
                    Text("Cerrar sesión")
                }
            }
            AuthUiState.SigningOut -> {
                CircularProgressIndicator()
                Text("Cerrando sesión y limpiando datos locales…")
            }
            else -> {
                Text("La cuenta no está disponible hasta iniciar sesión.")
            }
        }
    }
}
