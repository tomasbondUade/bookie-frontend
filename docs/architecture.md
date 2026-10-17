# Shared App Architecture

This is the initial architecture for the shared Kotlin Multiplatform module. It defines boundaries without implementing every feature.

## Source Set Responsibilities

| Source set | Responsibility |
| --- | --- |
| `commonMain` | Shared Compose UI, domain models and use cases, repository contracts, and common data-source abstractions/implementations. |
| `androidMain` | Android-specific integrations, including the Android Ktor engine and APIs that require Android. |
| `iosMain` | iOS-specific integrations, including the Darwin Ktor engine and APIs that require Apple platforms. |
| `androidApp` | Android application entry point and Android app configuration. |
| `iosApp` | Thin SwiftUI host that launches the shared Compose UI. |

Keep business rules and contracts platform-independent. Put platform API access behind shared abstractions and implement the platform-specific parts in their source sets. Do not put credentials or production endpoints in source code.

## Shared Package Proposal

Under `shared/src/commonMain/kotlin/org/bookie/app/`:

```text
presentation/
  navigation/
  search/
  library/
domain/
  model/
  repository/
  usecase/
data/
  remote/dto/
  local/
  mapper/
  repository/
```

`domain/repository` contains contracts; `data/repository` contains their implementations. Keep API DTOs separate from domain models, and map between them in `data/mapper`. Add feature packages as features are implemented instead of scaffolding every future screen now.

## Layer Responsibilities

- `presentation` renders UI, handles user input, and exposes loading, content, empty, and error states. It calls domain use cases, not HTTP or SQLDelight directly.
- `domain` owns business rules, shared models, use cases, and repository contracts. It does not depend on Ktor, SQLDelight, Compose UI, or platform APIs.
- `data` implements repository contracts, coordinates remote/local data sources, maps DTOs to domain models, and translates transport or storage details into domain-facing outcomes.
- Platform source sets provide integrations that need platform APIs, such as the Ktor engine or secure platform storage. The API remains the source of truth for server-owned state; local storage is a cache, not an offline write queue.

Dependencies point toward the domain: presentation depends on domain contracts, and data implements them. The application composition root supplies concrete data implementations to the presentation layer.

## Example Flow: Book Search

1. A search screen receives a query and asks `SearchBooks` to execute it.
2. `SearchBooks` calls the `BookRepository` contract with the query.
3. The data implementation asks a remote data source to perform the request through the configured Ktor client.
4. The data layer maps response DTOs to shared book models and returns a domain-facing result.
5. The screen presents loading, results, empty, or error state based on that result.

This flow is a boundary example, not a commitment to implement the search endpoint in this architecture task. Concrete models/contracts, client configuration, and endpoint integration remain in their respective issues.
