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

To install the debug app on a running emulator or connected Android device:

```bash
./gradlew :androidApp:installDebug
```

After installation, launch Bookie from the device's app launcher.

On macOS, open `iosApp/iosApp.xcodeproj` in Xcode, select the `iosApp` scheme and an iOS simulator or device, then run the app. Shared tests for the configured iOS simulator target can be run with:

```bash
./gradlew :shared:iosSimulatorArm64Test
```

The iOS Xcode build and simulator tests require macOS. The iOS simulator target configured by Gradle is `iosSimulatorArm64`.

## Local backend for mood search (Android debug)

Mood search (#50) calls the existing backend endpoint
`GET /books/mood?q=<free text>`. It does **not** use mock results.
The app's Search tab needs an API URL reachable from the device.

For local development, the checked-in Android **debug** resource sets
`bookie_api_base_url` to `http://127.0.0.1:3000`. This is a loopback
address, **not anyone's personal IP**. Each developer can run their
own Bookie backend and connect by USB:

1. Start the backend and PostgreSQL according to the
   [backend getting-started guide](https://github.com/tomasbondUade/bookie-backend/blob/main/docu/getting_started.md).
   Apply Prisma migrations and load the normal `seed:books` (which generates
   vector embeddings). The fast seed does not generate these embeddings.
2. Verify the API from the Mac/PC:
   `curl -G http://localhost:3000/books/mood --data-urlencode "q=Una aventura emocionante"`.
   It should return a JSON array (empty if no books with embeddings).
3. Connect an Android device by USB with USB debugging and run
   `adb reverse tcp:3000 tcp:3000`. You may need the full Android SDK
   platform-tools path if `adb` is not on `PATH`.
4. Run the `androidApp` **debug** configuration from Android Studio and
   open **Buscar**. The command `adb reverse` must be repeated after the
   device disconnects or restarts.

For an Android emulator, the same `adb reverse` approach works.
Alternatively, change the **debug** resource to
`http://10.0.2.2:3000` for the standard Android emulator mapping.

These settings live only in `androidApp/src/debug/`:
`AndroidManifest.xml` permits HTTP traffic for debug builds;
`res/values/bookie_api_config.xml` supplies the loopback API URL.
The main manifest remains unchanged and the main resource contains
**no production backend URL**. Never put tokens, credentials or
machine-specific LAN addresses into source control.

For deployed builds, configure a publicly reachable HTTPS backend URL
in the application environment before release. The library **save**
action additionally requires a logged-in session and an authenticated
shared client; discovery itself is public. See
[the mood discovery notes](docs/mood-exploration.md) for details.

## Contribution conventions

- Keep Kotlin packages under the base package `org.bookie.app`. Use lowercase, dot-separated package names that match the source directory.
- Put code that does not depend on a platform API in `commonMain`; keep Android and Apple APIs in `androidMain` and `iosMain`, respectively.
- Name work branches with the issue number and a short kebab-case description, for example `codex/1-kmp-base`. Keep changes on a branch and open a Pull Request targeting `main`.
- Use Conventional Commit style messages with a short scope when useful, for example `docs: document KMP targets` or `fix(library): handle empty state`.
- Keep this README focused on setup and repository conventions. See [the architecture guide](docs/architecture.md) for shared layers and package boundaries, and [the testing guide](docs/testing.md) for test scope and validation commands. For the cross-platform manual regression matrix, blockers, and recorded CI evidence, see [the end-to-end validation guide](docs/end-to-end-validation.md) and [the validation evidence log](docs/validation-evidence.md).
