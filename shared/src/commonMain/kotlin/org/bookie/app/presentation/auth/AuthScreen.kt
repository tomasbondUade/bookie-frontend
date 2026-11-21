package org.bookie.app.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.theme.BookieColors

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val password = remember { TextFieldState() }
    val confirmation = remember { TextFieldState() }
    var localError by remember { mutableStateOf<String?>(null) }
    var submissionStarted by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) { viewModel.restore() }
    LaunchedEffect(state) {
        if (state is AuthUiState.Submitting) {
            submissionStarted = true
        } else if (submissionStarted) {
            password.clearText()
            confirmation.clearText()
            localError = null
            submissionStarted = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        onBack?.let { back ->
            TextButton(onClick = back, modifier = Modifier.align(Alignment.Start)) {
                Text("Volver a Bookie")
            }
        }
        when (val current = state) {
            AuthUiState.Restoring -> {
                CircularProgressIndicator()
                Text("Revisando tu sesión", modifier = Modifier.padding(top = 12.dp))
            }
            AuthUiState.SigningOut -> {
                CircularProgressIndicator()
                Text("Cerrando sesión…", modifier = Modifier.padding(top = 12.dp))
            }
            is AuthUiState.SignedIn -> {
                Text("Sesión iniciada", style = MaterialTheme.typography.headlineSmall)
                current.email?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
            }
            is AuthUiState.EmailVerificationRequired -> {
                Text("Revisá tu correo", style = MaterialTheme.typography.headlineSmall)
                Text("Enviamos un enlace de confirmación a ${current.email}.", modifier = Modifier.padding(top = 8.dp))
                TextButton(onClick = { viewModel.chooseMode(AuthMode.SIGN_IN) }) {
                    Text("Volver al inicio de sesión")
                }
            }
            is AuthUiState.Form,
            is AuthUiState.Submitting -> {
                val mode = when (current) {
                    is AuthUiState.Form -> current.mode
                    is AuthUiState.Submitting -> current.mode
                }
                val form = current as? AuthUiState.Form
                val submitting = current is AuthUiState.Submitting

                Spacer(Modifier.height(24.dp))
                Text("BOOKIE", style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (mode == AuthMode.SIGN_IN) "Descubrí tu próxima lectura" else "¡Creá tu cuenta!",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                if (mode == AuthMode.SIGN_UP) Text("Leé. Conectá. Recomendá.")
                Spacer(Modifier.height(12.dp))
                if (form != null) {
                    if (mode == AuthMode.SIGN_UP) {
                        OutlinedTextField(
                            value = form.username,
                            onValueChange = viewModel::updateUsername,
                            label = { Text("Nombre de usuario") },
                            singleLine = true,
                            enabled = !submitting,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    OutlinedTextField(
                        value = form.email,
                        onValueChange = viewModel::updateEmail,
                        label = { Text("Correo electrónico") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        enabled = !submitting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedSecureTextField(
                        state = password,
                        label = { Text("Contraseña") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        enabled = !submitting,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                    if (mode == AuthMode.SIGN_UP) {
                        OutlinedSecureTextField(
                            state = confirmation,
                            label = { Text("Confirmar contraseña") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            enabled = !submitting,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    form.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    }
                    localError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        onClick = {
                            when {
                                mode == AuthMode.SIGN_UP && password.text.toString() != confirmation.text.toString() ->
                                    localError = "Las contraseñas no coinciden."
                                else -> {
                                    localError = null
                                    viewModel.submit(password.text.toString())
                                }
                            }
                        },
                        enabled = !submitting,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BookieColors.LightAccent),
                    ) {
                        if (submitting) CircularProgressIndicator()
                        else Text(if (mode == AuthMode.SIGN_IN) "Iniciar sesión" else "CREAR CUENTA")
                    }
                } else {
                    CircularProgressIndicator()
                }
                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(if (mode == AuthMode.SIGN_IN) "¿No tenés cuenta?" else "¿Ya tenés cuenta?")
                    TextButton(
                        onClick = {
                            password.clearText()
                            viewModel.chooseMode(if (mode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN)
                        },
                        enabled = !submitting,
                    ) {
                        Text(if (mode == AuthMode.SIGN_IN) "Registrate" else "Ingresá")
                    }
                }
            }
        }
    }
}
