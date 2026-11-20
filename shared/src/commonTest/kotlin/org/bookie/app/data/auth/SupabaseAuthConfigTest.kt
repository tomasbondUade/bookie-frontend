package org.bookie.app.data.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SupabaseAuthConfigTest {
    private val publicKey = "sb_publishable_test_example"

    @Test
    fun `remote supabase always requires https`() {
        assertFailsWith<IllegalArgumentException> {
            SupabaseAuthConfig("http://example.com:54321", publicKey, allowLoopbackHttp = true)
        }
        assertEquals(
            "https://demo.supabase.co",
            SupabaseAuthConfig("https://demo.supabase.co", publicKey).authBaseUrl,
        )
    }

    @Test
    fun `local http is rejected by default and accepted only on explicit debug opt in`() {
        assertFailsWith<IllegalArgumentException> {
            SupabaseAuthConfig("http://127.0.0.1:54321", publicKey)
        }
        assertEquals(
            "http://127.0.0.1:54321",
            SupabaseAuthConfig("http://127.0.0.1:54321/", publicKey, allowLoopbackHttp = true)
                .authBaseUrl,
        )
    }

    @Test
    fun `secret keys are always rejected even in local mode`() {
        assertFailsWith<IllegalArgumentException> {
            SupabaseAuthConfig(
                "http://127.0.0.1:54321",
                "sb_secret_fake_test",
                allowLoopbackHttp = true,
            )
        }
    }
}
