package org.bookie.app

import io.ktor.http.URLProtocol
import io.ktor.http.Url

data class AppRuntimeConfig(
    val apiBaseUrl: String,
    val supabaseUrl: String,
    val supabasePublishableKey: String,
    val allowLocalHttp: Boolean = false,
) {
    val hasApi: Boolean get() = apiBaseUrl.isNotBlank()
    val hasAuth: Boolean
        get() = hasApi && supabaseUrl.isNotBlank() && supabasePublishableKey.isNotBlank()

    init {
        validateEndpoint(apiBaseUrl, "API")
        validateEndpoint(supabaseUrl, "Supabase")
        require(!supabasePublishableKey.startsWith("sb_secret_", ignoreCase = true)) {
            "La app solo puede incluir una clave pública de Supabase."
        }
        require(!supabasePublishableKey.contains("service_role", ignoreCase = true)) {
            "La app no puede incluir una clave service_role."
        }
    }

    private fun validateEndpoint(value: String, name: String) {
        if (value.isBlank()) return
        val endpoint = Url(value)
        val localHttp = allowLocalHttp && endpoint.protocol == URLProtocol.HTTP &&
            endpoint.host.isLocalNetworkHost()
        require(endpoint.protocol == URLProtocol.HTTPS || localHttp) {
            "$name requiere HTTPS; HTTP solo se permite para desarrollo local."
        }
        require(endpoint.host.isNotBlank() && endpoint.user.isNullOrBlank() &&
            endpoint.password.isNullOrBlank() && endpoint.parameters.isEmpty() &&
            endpoint.fragment.isEmpty()) {
            "La URL de $name no puede incluir credenciales, parámetros ni fragmentos."
        }
    }
}

private fun String.isLocalNetworkHost(): Boolean {
    val host = lowercase().removePrefix("[").removeSuffix("]")
    if (host == "localhost" || host.endsWith(".localhost") || host == "::1" || host == "10.0.2.2") {
        return true
    }
    val octets = host.split('.').map(String::toIntOrNull)
    if (octets.size != 4 || octets.any { it == null || it !in 0..255 }) return false
    val values = octets.filterNotNull()
    val first = values[0]
    val second = values[1]
    return first == 10 || first == 127 ||
        (first == 172 && second in 16..31) ||
        (first == 192 && second == 168) ||
        (first == 169 && second == 254)
}
