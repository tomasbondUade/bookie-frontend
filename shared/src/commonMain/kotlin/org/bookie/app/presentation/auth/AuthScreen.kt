package org.bookie.app.presentation.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.presentation.components.BookieGradientButton
import org.bookie.app.presentation.components.BookieLibraryBackdrop
import org.bookie.app.presentation.components.BookieMark

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.restore() }
    BookieAuthContent(state, viewModel::chooseMode, viewModel::updateEmail,
        viewModel::updateUsername, viewModel::submit, modifier, onBack)
}

/** The form can be previewed without a session or network client. */
@Composable
fun BookieAuthContent(
    state: AuthUiState,
    onModeChange: (AuthMode) -> Unit,
    onEmailChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val password = remember { TextFieldState() }
    val confirmation = remember { TextFieldState() }
    var localError by remember { mutableStateOf<String?>(null) }
    var submissionStarted by remember { mutableStateOf(false) }
    var lastForm by remember { mutableStateOf(AuthUiState.Form()) }
    LaunchedEffect(state) {
        (state as? AuthUiState.Form)?.let { lastForm = it }
        if (state is AuthUiState.Submitting) submissionStarted = true
        else if (submissionStarted) {
            password.clearText()
            confirmation.clearText()
            localError = null
            submissionStarted = false
        }
    }
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            BookieLibraryBackdrop(Modifier.fillMaxSize(), darken = .58f)
            Column(
                Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
                    .padding(horizontal = 26.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                onBack?.let { back ->
                    TextButton(onClick = back, modifier = Modifier.align(Alignment.Start)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(18.dp))
                        Text("  Volver a Bookie")
                    }
                }
                Spacer(Modifier.height(8.dp))
                BookieMark(Modifier.size(58.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Bookie", fontSize = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
                    Text("Leé. Conectá. Recomendá.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                when (val current = state) {
                    AuthUiState.Restoring -> {
                        Spacer(Modifier.height(28.dp))
                        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                        Text("Preparando tu rincón de lectura…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is AuthUiState.SignedIn -> {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Ya estás en casa", style = MaterialTheme.typography.headlineSmall)
                        current.email?.let { Text(it) }
                    }
                    is AuthUiState.EmailVerificationRequired -> {
                        Spacer(Modifier.height(16.dp))
                        Icon(Icons.Default.Email, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Revisá tu correo", style = MaterialTheme.typography.headlineSmall)
                        Text("Enviamos un enlace de confirmación a ${current.email}.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        BookieGradientButton("Volver al inicio de sesión", { onModeChange(AuthMode.SIGN_IN) }, Modifier.fillMaxWidth())
                    }
                    is AuthUiState.Form, is AuthUiState.Submitting -> {
                        val form = (current as? AuthUiState.Form) ?: lastForm
                        val mode = (current as? AuthUiState.Submitting)?.mode ?: form.mode
                        val submitting = current is AuthUiState.Submitting
                        val signUp = mode == AuthMode.SIGN_UP
                        Spacer(Modifier.height(4.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (signUp) "Tu historia empieza acá" else "Un nuevo capítulo te espera", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                            Text(if (signUp) "Creá tu cuenta y encontrá tu próxima lectura." else "Entrá y volvé a lo que te gusta.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                        val fieldColors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .9f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .85f),
                            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .85f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        )
                        Column(Modifier.widthIn(max = 440.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (signUp) OutlinedTextField(
                                value = form.username, onValueChange = onUsernameChange,
                                label = { Text("Nombre de usuario") },
                                leadingIcon = { Icon(Icons.Default.PersonOutline, null) },
                                singleLine = true, enabled = !submitting,
                                shape = RoundedCornerShape(18.dp), colors = fieldColors,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = form.email, onValueChange = onEmailChange,
                                label = { Text("Correo electrónico") },
                                leadingIcon = { Icon(Icons.Default.Email, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true, enabled = !submitting,
                                shape = RoundedCornerShape(18.dp), colors = fieldColors,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedSecureTextField(
                                state = password, label = { Text("Contraseña") },
                                leadingIcon = { Icon(Icons.Default.Lock, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                enabled = !submitting, shape = RoundedCornerShape(18.dp),
                                colors = fieldColors, modifier = Modifier.fillMaxWidth(),
                            )
                            if (signUp) OutlinedSecureTextField(
                                state = confirmation, label = { Text("Confirmar contraseña") },
                                leadingIcon = { Icon(Icons.Default.Lock, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                enabled = !submitting, shape = RoundedCornerShape(18.dp),
                                colors = fieldColors, modifier = Modifier.fillMaxWidth(),
                            )
                            val error = localError ?: (current as? AuthUiState.Form)?.error
                            error?.let {
                                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.error.copy(alpha = .12f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .35f))) {
                                    Text(it, Modifier.padding(14.dp).semantics { liveRegion = LiveRegionMode.Polite }, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            BookieGradientButton(
                                text = if (signUp) "Crear mi cuenta" else "Iniciar sesión",
                                onClick = {
                                    if (signUp && password.text.toString() != confirmation.text.toString()) {
                                        localError = "Las contraseñas no coinciden."
                                    } else {
                                        localError = null
                                        onSubmit(password.text.toString())
                                    }
                                },
                                loading = submitting,
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Text(if (signUp) "¿Ya tenés cuenta?" else "¿Todavía no tenés cuenta?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(
                                    onClick = {
                                        password.clearText()
                                        confirmation.clearText()
                                        localError = null
                                        onModeChange(if (signUp) AuthMode.SIGN_IN else AuthMode.SIGN_UP)
                                    },
                                    enabled = !submitting,
                                ) { Text(if (signUp) "Ingresá" else "Registrate", style = MaterialTheme.typography.labelLarge) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
