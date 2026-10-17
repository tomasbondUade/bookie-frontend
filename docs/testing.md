# Testing and Local Validation

Tests should verify behavior at the narrowest useful layer and should not require production credentials or a live backend.

## Test Scope

| Area | What to verify | Preferred source set |
| --- | --- | --- |
| Domain | Use-case rules, input validation, and state transitions | `commonTest` |
| Models and mapping | DTO parsing, optional fields, unknown fields, and DTO-to-domain mapping | `commonTest` |
| Repositories | Remote success/failure, cache fallback, and server-confirmed writes using fakes or a mock HTTP engine | `commonTest` |
| Android integration | Android-specific behavior that can run on a local JVM | `androidHostTest` |
| iOS integration | Apple-platform behavior and simulator integration | `iosTest` |

For Online First behavior, test that cached data can be read without a connection, a successful remote response refreshes the cache, and failed writes are not shown as saved. The MVP does not queue offline writes. Authentication tests must use fake sessions/tokens and must never log or persist real credentials as fixtures.

Use a Ktor mock engine for HTTP parsing and error tests. Unit tests should not depend on the deployed Bookie API; endpoint smoke tests, if added later, must be explicit and use a non-production environment.

## Validation Commands

The CI build job runs these checks on Linux with Java 17:

```bash
./gradlew --no-daemon --stacktrace \
  :shared:compileCommonMainKotlinMetadata \
  :shared:testAndroidHostTest \
  :androidApp:lintDebug \
  :androidApp:assembleDebug
```

Run one task while iterating, for example:

```bash
./gradlew :shared:testAndroidHostTest
```

The iOS simulator test requires macOS and Xcode:

```bash
./gradlew :shared:iosSimulatorArm64Test
```

On Linux, Gradle may report iOS simulator tasks as disabled; that is a host limitation, not a passing iOS test. See the README for SDK and JDK requirements.

## Security Checks

CodeQL and Dependency Review run in GitHub Actions; Dependency Review is pull-request-only. This repository does not currently define a local command for those checks or a local secret-scanning tool. Treat the corresponding GitHub checks as required validation and do not add real tokens, passwords, or production configuration to test data.
