import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use(::load)
}

fun appConfig(property: String, environment: String): String =
    System.getenv(environment)?.takeIf(String::isNotBlank)
        ?: localProperties.getProperty(property, "")

fun buildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val bookieApiBaseUrl = appConfig("bookieApiBaseUrl", "BOOKIE_API_BASE_URL")
val supabaseUrl = appConfig("supabaseUrl", "SUPABASE_URL")
val supabasePublishableKey = appConfig("supabasePublishableKey", "SUPABASE_PUBLISHABLE_KEY")
val bookieCommunityId = appConfig("bookieCommunityId", "BOOKIE_COMMUNITY_ID")

require(!supabasePublishableKey.startsWith("sb_secret_", ignoreCase = true)) {
    "SUPABASE_PUBLISHABLE_KEY must not be a Supabase secret key."
}
require(!supabasePublishableKey.contains("service_role", ignoreCase = true)) {
    "SUPABASE_PUBLISHABLE_KEY must not be a service_role key."
}

buildscript {
    dependencies {
        constraints {
            classpath("org.bouncycastle:bcpkix-jdk18on:1.86")
            classpath("org.bouncycastle:bcprov-jdk18on:1.86")
            classpath("com.google.guava:guava:33.7.2-jre")
            classpath("io.opentelemetry:opentelemetry-api:1.67.0")
            classpath("io.opentelemetry:opentelemetry-context:1.67.0")
            classpath("org.apache.commons:commons-lang3:3.21.0")
            classpath("org.apache.httpcomponents:httpclient:4.5.14")
        }
    }
}

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "org.bookie.app"
    compileSdk {
        version = release(libs.versions.android.compileSdk.get().toInt()) {
            minorApiLevel = libs.versions.android.compileSdkMinor.get().toInt()
        }
    }

    defaultConfig {
        applicationId = "org.bookie.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "BOOKIE_API_BASE_URL", buildConfigString(bookieApiBaseUrl))
        buildConfigField("String", "SUPABASE_URL", buildConfigString(supabaseUrl))
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", buildConfigString(supabasePublishableKey))
        buildConfigField("String", "BOOKIE_COMMUNITY_ID", buildConfigString(bookieCommunityId))
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
