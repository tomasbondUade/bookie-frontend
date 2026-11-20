package org.bookie.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.bookie.app.data.auth.SupabaseAuthConfig
import org.bookie.app.data.auth.createAndroidAuthClients
import org.bookie.app.data.local.createAndroidLibraryCache

class MainActivity : ComponentActivity() {
    private lateinit var runtimeHolder: AppRuntimeHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.WHITE, android.graphics.Color.WHITE),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.WHITE, android.graphics.Color.WHITE),
        )
        super.onCreate(savedInstanceState)

        runtimeHolder = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (!modelClass.isAssignableFrom(AppRuntimeHolder::class.java)) {
                        error("Unsupported ViewModel: ${modelClass.name}")
                    }
                    @Suppress("UNCHECKED_CAST")
                    return AppRuntimeHolder(createRuntime()) as T
                }
            },
        )[AppRuntimeHolder::class.java]

        setContent {
            App(runtimeHolder.runtime?.featureViewModels)
        }
    }

    private fun createRuntime(): AppRuntime? {
        val config = AppRuntimeConfig(
            apiBaseUrl = BuildConfig.BOOKIE_API_BASE_URL,
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabasePublishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
            allowLocalHttp = BuildConfig.DEBUG,
        )
        val authClients = if (config.hasAuth) {
            createAndroidAuthClients(
                context = applicationContext,
                supabaseConfig = SupabaseAuthConfig(
                    url = config.supabaseUrl,
                    publishableKey = config.supabasePublishableKey,
                    allowLoopbackHttp = config.allowLocalHttp,
                ),
                bookieBaseUrl = config.apiBaseUrl,
            )
        } else {
            null
        }
        return createAppRuntime(
            config = config,
            cache = createAndroidLibraryCache(applicationContext),
            authClients = authClients,
        )
    }
}

private class AppRuntimeHolder(val runtime: AppRuntime?) : ViewModel() {
    override fun onCleared() {
        runtime?.close()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
