package org.bookie.app

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppRuntimeConfigTest {
    @Test
    fun `production endpoints require https and allow only public supabase keys`() {
        val config = AppRuntimeConfig(
            apiBaseUrl = "https://api.example.test",
            supabaseUrl = "https://auth.example.test",
            supabasePublishableKey = "sb_publishable_test",
        )

        assertTrue(config.hasApi)
        assertTrue(config.hasAuth)
    }

    @Test
    fun `remote http endpoints are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            AppRuntimeConfig("http://api.example.test", "", "")
        }
    }

    @Test
    fun `debug may use local network api over http`() {
        val config = AppRuntimeConfig(
            apiBaseUrl = "http://10.0.2.2:3000",
            supabaseUrl = "",
            supabasePublishableKey = "",
            allowLocalHttp = true,
        )

        assertTrue(config.hasApi)
        assertFalse(config.hasAuth)
    }

    @Test
    fun `credentials and query strings are rejected from service urls`() {
        assertFailsWith<IllegalArgumentException> {
            AppRuntimeConfig("https://user:pass@api.example.test", "", "")
        }
        assertFailsWith<IllegalArgumentException> {
            AppRuntimeConfig("https://api.example.test?token=secret", "", "")
        }
    }

    @Test
    fun `service role keys are rejected from the mobile runtime`() {
        assertFailsWith<IllegalArgumentException> {
            AppRuntimeConfig(
                apiBaseUrl = "https://api.example.test",
                supabaseUrl = "https://auth.example.test",
                supabasePublishableKey = "service_role_fake_test",
            )
        }
        assertFailsWith<IllegalArgumentException> {
            AppRuntimeConfig(
                apiBaseUrl = "https://api.example.test",
                supabaseUrl = "https://auth.example.test",
                supabasePublishableKey = "sb_secret_fake_test",
            )
        }
    }
}
