package org.bookie.app.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LibrarySelectionDialogsTest {
    @Test
    fun `date picker values round trip to backend ISO timestamps`() {
        val millis = "2026-11-19T10:30:00Z".toUtcDateMillis()

        assertEquals("2026-11-19T00:00:00.000Z", millis?.toIsoDateTime())
    }

    @Test
    fun `date picker preserves leap day`() {
        val millis = "2024-02-29".toUtcDateMillis()

        assertEquals("2024-02-29T00:00:00.000Z", millis?.toIsoDateTime())
    }

    @Test
    fun `invalid dates are not passed to the picker`() {
        assertNull("2025-02-29".toUtcDateMillis())
        assertNull("not-a-date".toUtcDateMillis())
    }
}
