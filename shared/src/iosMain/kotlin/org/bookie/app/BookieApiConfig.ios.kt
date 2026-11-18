package org.bookie.app

import androidx.compose.runtime.Composable
import platform.Foundation.NSBundle

@Composable
internal actual fun platformBookieApiBaseUrl(): String? =
    (NSBundle.mainBundle.objectForInfoDictionaryKey("BOOKIE_API_BASE_URL") as? String)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
