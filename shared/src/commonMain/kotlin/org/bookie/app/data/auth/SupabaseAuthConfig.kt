package org.bookie.app.data.auth

import io.ktor.http.URLProtocol
import io.ktor.http.Url

/** Only a public/anon key is allowed in the mobile app. Never use a service_role key. */
data class SupabaseAuthConfig(
    val url: String,
    val publishableKey: String,
    // Opt-in exclusively for platform debug builds connecting to local Supabase over adb reverse.
    val allowLoopbackHttp: Boolean = false,
) {
    val authBaseUrl: String = url.trimEnd('/')

    init {
        val parsed = Url(url)
        val isLocalDebug = allowLoopbackHttp &&
            parsed.protocol == URLProtocol.HTTP &&
            parsed.host in setOf("127.0.0.1", "localhost")
        require(parsed.protocol == URLProtocol.HTTPS || isLocalDebug) {
            "Supabase requiere HTTPS (HTTP solo se permite en loopback para desarrollo)."
        }
        require(parsed.host.isNotBlank() && parsed.user.isNullOrBlank() &&
            parsed.password.isNullOrBlank() && parsed.parameters.isEmpty() &&
            parsed.fragment.isEmpty()) { "URL de Supabase inválida." }
        require(publishableKey.isNotBlank() &&
            !publishableKey.startsWith("sb_secret_") &&
            !publishableKey.contains("service_role", ignoreCase = true)) {
            "Configurá únicamente la clave pública de Supabase."
        }
    }
}
