package org.bookie.app.data.local

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import org.bookie.app.data.local.db.BookieCacheDatabase

fun createIosLibraryCache(): LibraryCache =
    SqlDelightLibraryCache(
        NativeSqliteDriver(BookieCacheDatabase.Schema, "bookie-cache.db"),
    )
