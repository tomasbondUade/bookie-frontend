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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val password = remember { TextFieldState() }
    var submissionStarted by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) { viewModel.restore() }
    LaunchedEffect(state) {
        if (state is AuthUiState.Submitting) {
            submissionStarted = true
        } else if (submissionStarted) {
            password.clearText()
            submissionStarted = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        when (val current = state) {
            AuthUiState.Restoring -> {
                CircularProgressIndicator()
                Text("Revisando tu sesión", modifier = Modifier.padding(top = 12.dp))
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

                Text(
                    if (mode == AuthMode.SIGN_IN) "Iniciar sesión" else "Crear cuenta",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(20.dp))
                if (form != null) {
                    OutlinedTextField(
                        value = form.email,
                        onValueChange = viewModel::updateEmail,
                        label = { Text("Correo electrónico") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        enabled = !submitting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (mode == AuthMode.SIGN_UP) {
                        OutlinedTextField(
                            value = form.username,
                            onValueChange = viewModel::updateUsername,
                            label = { Text("Nombre de usuario") },
                            singleLine = true,
                            enabled = !submitting,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        )
                    }
                    OutlinedSecureTextField(
                        state = password,
                        label = { Text("Contraseña") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        enabled = !submitting,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                    form.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    }
                    Button(
                        onClick = { viewModel.submit(password.text.toString()) },
                        enabled = !submitting,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    ) {
                        if (submitting) CircularProgressIndicator()
                        else Text(if (mode == AuthMode.SIGN_IN) "Entrar" else "Registrarme")
                    }
                } else {
                    CircularProgressIndicator()
                }
                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
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
