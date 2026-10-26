package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android

actual fun createPlatformHttpClient(config: BookieHttpClientConfig): HttpClient =
    createBookieHttpClient(Android, config)
