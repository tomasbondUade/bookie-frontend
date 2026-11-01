package org.bookie.app.data.auth

import io.ktor.client.engine.darwin.Darwin
import org.bookie.app.data.remote.BookieHttpClientConfig

/** Compose once in the iOS application scope; Keychain survives process restarts. */
fun createIosAuthClients(
    supabaseConfig: SupabaseAuthConfig,
    bookieBaseUrl: String,
): AuthClientBundle = assembleAuthClients(
    supabaseConfig = supabaseConfig,
    bookieBaseUrl = bookieBaseUrl,
    store = IosKeychainSessionStore(),
    supabaseClient = createSupabaseHttpClient(Darwin),
    syncClient = createBookieUserSyncHttpClient(Darwin, BookieHttpClientConfig(bookieBaseUrl)),
)
