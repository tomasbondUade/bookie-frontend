# Bookie Mobile

Bookie Mobile is a Kotlin Multiplatform app targeting Android and iOS. The shared module uses Compose Multiplatform for UI that can run on both platforms.

## Project layout

| Path | Responsibility |
| --- | --- |
| `androidApp/` | Android application module and Android entry point. |
| `iosApp/` | Xcode project and thin SwiftUI host for the shared application. |
| `shared/` | Shared Kotlin code, Compose UI, platform source sets, and shared tests. |
| `gradle/libs.versions.toml` | Central version catalog for Gradle plugins and dependencies. |

## Kotlin source sets and targets

The `shared` module currently configures these Kotlin source sets:

| Source set | Use |
| --- | --- |
| `commonMain` | Code compiled for all supported targets, including shared Compose UI. |
| `androidMain` | Android-specific implementations and APIs. |
| `iosMain` | iOS-specific implementations and the Compose view-controller entry point. |
| `commonTest` | Tests that run against shared Kotlin code. |
| `androidHostTest` | Shared-module tests executed on the local JVM. |
| `iosTest` | Tests for the iOS target. |

Configured targets are Android (minimum SDK 24, compile/target SDK 36), iOS device (`iosArm64`), and iOS simulator (`iosSimulatorArm64`, deployment target 18.2). The iOS simulator target currently supports Apple Silicon; an Intel simulator target is not configured.
