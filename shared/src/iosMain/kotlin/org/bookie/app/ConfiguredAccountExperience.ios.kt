package org.bookie.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import org.bookie.app.data.auth.SupabaseAuthConfig
import org.bookie.app.data.auth.createIosAuthClients
import org.bookie.app.data.local.createIosLibraryCache
import platform.Foundation.NSBundle

@Composable
internal actual fun rememberConfiguredAccountExperience(): ConfiguredAccountExperience? {
    val backendUrl = platformBookieApiBaseUrl()
    val supabaseUrl = (NSBundle.mainBundle.objectForInfoDictionaryKey(
        "BOOKIE_SUPABASE_URL",
    ) as? String)?.trim()?.takeIf(String::isNotEmpty)
    val publishableKey = (NSBundle.mainBundle.objectForInfoDictionaryKey(
        "BOOKIE_SUPABASE_PUBLISHABLE_KEY",
    ) as? String)?.trim()?.takeIf(String::isNotEmpty)

    if (backendUrl.isNullOrBlank() || supabaseUrl.isNullOrBlank() || publishableKey.isNullOrBlank()) {
        return null
    }
    val experience = remember(backendUrl, supabaseUrl, publishableKey) {
        runCatching {
            val clients = createIosAuthClients(
                SupabaseAuthConfig(supabaseUrl, publishableKey),
                backendUrl,
            )
            ConfiguredAccountExperience(clients, createIosLibraryCache())
        }.getOrNull()
    }
    if (experience != null) {
        DisposableEffect(experience) { onDispose { experience.close() } }
    }
    return experience
}
