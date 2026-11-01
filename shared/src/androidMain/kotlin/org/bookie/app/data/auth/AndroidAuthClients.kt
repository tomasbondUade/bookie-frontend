package org.bookie.app.data.auth

import android.content.Context
import io.ktor.client.engine.android.Android
import org.bookie.app.data.remote.BookieHttpClientConfig

/** Configure public Supabase URL/key and Bookie API URL at the application composition root. */
fun createAndroidAuthClients(
    context: Context,
    supabaseConfig: SupabaseAuthConfig,
    bookieBaseUrl: String,
): AuthClientBundle = assembleAuthClients(
    supabaseConfig = supabaseConfig,
    bookieBaseUrl = bookieBaseUrl,
    store = AndroidSecureSessionStore(context),
    supabaseClient = createSupabaseHttpClient(Android),
    syncClient = createBookieUserSyncHttpClient(Android, BookieHttpClientConfig(bookieBaseUrl)),
)
