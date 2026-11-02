package org.bookie.app.data.auth

import io.ktor.client.HttpClient
import org.bookie.app.data.remote.AccessTokenProvider
import org.bookie.app.data.remote.BookieHttpClientConfig
import org.bookie.app.data.remote.createPlatformHttpClient

/**
 * A single shared repository supplies the JWT to all Bookie API calls.
 * Close the bundle when its owning application scope is destroyed.
 */
class AuthClientBundle(
    val repository: SupabaseAuthRepository,
    val bookieClient: HttpClient,
    private val supabaseClient: HttpClient,
    private val syncClient: HttpClient,
) {
    fun close() {
        bookieClient.close()
        supabaseClient.close()
        syncClient.close()
    }
}

fun assembleAuthClients(
    supabaseConfig: SupabaseAuthConfig,
    bookieBaseUrl: String,
    store: SecureSessionStore,
    supabaseClient: HttpClient,
    syncClient: HttpClient,
): AuthClientBundle {
    val repository = SupabaseAuthRepository(
        remote = KtorSupabaseAuthRemoteDataSource(supabaseClient, supabaseConfig),
        store = store,
        userSync = KtorBookieUserSyncRemoteDataSource(syncClient),
    )
    val bookieClient = createPlatformHttpClient(
        BookieHttpClientConfig(baseUrl = bookieBaseUrl, accessTokenProvider = repository),
    )
    return AuthClientBundle(repository, bookieClient, supabaseClient, syncClient)
}
