package org.bookie.app.presentation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppDestinationTest {
    @Test
    fun `defines the five primary app destinations in navigation order`() {
        assertEquals(
            listOf(
                AppDestination.HOME,
                AppDestination.SEARCH,
                AppDestination.LIBRARY,
                AppDestination.COMMUNITY,
                AppDestination.PROFILE,
            ),
            AppDestination.entries,
        )
    }

    @Test
    fun `routes are stable and unique`() {
        val routes = AppDestination.entries.map(AppDestination::route)

        assertEquals(listOf("home", "search", "library", "community", "profile"), routes)
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun `resolves known route and rejects unknown or absent route`() {
        assertEquals(AppDestination.LIBRARY, AppDestination.fromRoute("library"))
        assertNull(AppDestination.fromRoute("settings"))
        assertNull(AppDestination.fromRoute(null))
    }
}
