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

## Requirements

- JDK 21. The Gradle daemon JVM toolchain is configured in `gradle/gradle-daemon-jvm.properties`.
- Android SDK 36 for Android builds.
- macOS with Xcode for building and running the iOS app. The project uses an iOS 18.2 deployment target.

## Build and test

From the repository root, build the Android debug app and run shared-module tests on the local JVM:

```bash
./gradlew :androidApp:assembleDebug
./gradlew :shared:testAndroidHostTest
```

On macOS, open `iosApp/iosApp.xcodeproj` in Xcode, select the `iosApp` scheme and an iOS simulator or device, then run the app. Shared tests for the configured iOS simulator target can be run with:

```bash
./gradlew :shared:iosSimulatorArm64Test
```

The iOS Xcode build and simulator tests require macOS. The iOS simulator target configured by Gradle is `iosSimulatorArm64`.
