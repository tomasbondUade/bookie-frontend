package org.bookie.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun platformBookieApiBaseUrl(): String? {
    val context = LocalContext.current
    // App resources may be supplied independently of the shared library.
    val id = context.resources.getIdentifier(
        "bookie_api_base_url",
        "string",
        context.packageName,
    )
    return if (id != 0) context.getString(id).trim().takeIf(String::isNotEmpty) else null
}
