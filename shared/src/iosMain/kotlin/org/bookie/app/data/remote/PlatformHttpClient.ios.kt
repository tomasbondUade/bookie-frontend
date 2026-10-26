package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun createPlatformHttpClient(config: BookieHttpClientConfig): HttpClient =
    createBookieHttpClient(Darwin, config)
