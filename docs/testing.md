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

## Feature Scenarios

These scenarios define the expected test coverage as each feature is implemented; they do not imply that every scenario is already covered by an existing test.

| Feature | Scenarios to verify | Test approach |
| --- | --- | --- |
| Search | Blank input makes no request; valid response maps to domain books; empty response becomes an empty-result UI state; network, HTTP, timeout, and malformed-response failures remain recoverable. | Test use cases and ViewModels with fakes; test Ktor parsing and errors with `MockEngine` in `commonTest`. |
| Library | Parse the `{ data, meta }` page and filters; display cached data when available; refresh cache after a successful remote read; show stale/empty/error states when offline; do not report a write as saved when the server rejects it. | Test repository coordination with fake local/remote sources and test `Flow` emissions when the local cache is implemented. Keep requests and mapping tests in `commonTest`. |
| Reading progress | Validate only rules supported by the contract; persist the current snapshot separately from a reading session; surface partial failure if only one request succeeds; reuse an idempotency key only when retrying the same session creation. | Test domain validation with pure unit tests and repository requests/errors with fakes or `MockEngine` in `commonTest`. Do not infer a maximum page or percentage from `pageCount`, which the API does not provide. |
| Presentation state | Verify loading, content, empty, and error transitions; retry returns to loading and then reflects the operation result. | Test ViewModels with fake use cases and `kotlinx-coroutines-test`; assert `StateFlow.value` where possible. |

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
