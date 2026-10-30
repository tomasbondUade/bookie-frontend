package org.bookie.app.data.auth

import io.ktor.http.URLProtocol
import io.ktor.http.Url

/** Only a public/anon key is allowed in the mobile app. Never use a service_role key. */
data class SupabaseAuthConfig(
    val url: String,
    val publishableKey: String,
) {
    val authBaseUrl: String = url.trimEnd('/')

    init {
        val parsed = Url(url)
        require(parsed.protocol == URLProtocol.HTTPS) { "Supabase requiere HTTPS." }
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
