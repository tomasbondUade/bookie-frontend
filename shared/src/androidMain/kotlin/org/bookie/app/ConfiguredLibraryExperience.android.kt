package org.bookie.app

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.bookie.app.data.auth.SupabaseAuthConfig
import org.bookie.app.data.auth.createAndroidAuthClients
import org.bookie.app.data.local.createAndroidLibraryCache

@Composable
internal actual fun rememberConfiguredLibraryExperience(): ConfiguredLibraryExperience? {
    val context = LocalContext.current.applicationContext
    val bookieUrl = platformBookieApiBaseUrl()
    val supabaseUrl = remember(context) { context.configString("bookie_supabase_url") }
    val publishableKey = remember(context) { context.configString("bookie_supabase_publishable_key") }

    if (bookieUrl.isNullOrBlank() || supabaseUrl.isNullOrBlank() || publishableKey.isNullOrBlank()) {
        return null
    }

    val experience = remember(context, bookieUrl, supabaseUrl, publishableKey) {
        runCatching {
            val clients = createAndroidAuthClients(
                context = context,
                supabaseConfig = SupabaseAuthConfig(
                    url = supabaseUrl,
                    publishableKey = publishableKey,
                    allowLoopbackHttp = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0,
                ),
                bookieBaseUrl = bookieUrl,
            )
            ConfiguredLibraryExperience(clients, createAndroidLibraryCache(context))
        }.getOrNull()
    }
    if (experience != null) {
        DisposableEffect(experience) {
            onDispose { experience.close() }
        }
    }
    return experience
}

private fun Context.configString(name: String): String? {
    val id = resources.getIdentifier(name, "string", packageName)
    return if (id == 0) null else getString(id).trim().takeIf(String::isNotEmpty)
}
