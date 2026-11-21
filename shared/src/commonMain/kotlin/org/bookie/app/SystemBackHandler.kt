package org.bookie.app

import androidx.compose.runtime.Composable

@Composable
internal expect fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit)
