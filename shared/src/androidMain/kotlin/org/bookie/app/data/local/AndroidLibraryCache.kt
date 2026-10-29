package org.bookie.app.data.local

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.bookie.app.data.local.db.BookieCacheDatabase

/** Application context only; database file persists between launches. */
fun createAndroidLibraryCache(context: Context): LibraryCache =
    SqlDelightLibraryCache(
        AndroidSqliteDriver(
            schema = BookieCacheDatabase.Schema,
            context = context.applicationContext,
            name = "bookie-cache.db",
        ),
    )
