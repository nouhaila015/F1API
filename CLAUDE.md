# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./mvnw spring-boot:run        # Start the server (port 8000)
./mvnw test                   # Run all tests
./mvnw clean package          # Build JAR
./mvnw test -Dtest=ClassName  # Run a single test class
```

## Architecture

This is a Spring Boot 3 / Java 21 API that aggregates Formula 1 data from the external [OpenF1 API](https://api.openf1.org/v1) and computes standings and statistics.

### Layer Flow

```
Controller → Service → OpenF1Client → https://api.openf1.org/v1
```

- **`controller/`** — REST endpoints. `RaceController` handles `/api/races/**`, `StandingsController` handles `/api/standings/**`.
- **`service/`** — Business logic. `SessionService` fetches and filters sessions; `RaceService` aggregates race data; `ScoringService` computes driver and constructor standings.
- **`client/OpenF1Client`** — Spring WebClient wrapper. All external HTTP calls go through here. Converts 4xx/5xx into `OpenF1Exception`.
- **`model/`** — Java records mapping raw OpenF1 API JSON (snake_case via Jackson).
- **`dto/`** — Response objects returned to API consumers (may contain computed/aggregated fields).
- **`utils/F1ScoringUtils`** — F1 point arrays (Race: top 10, Sprint: top 8) and fastest-lap bonus logic.

### Key Design Decisions

**Parallel fetching with virtual threads**: `RaceService` uses `Executors.newVirtualThreadPerTaskExecutor()` with `CompletableFuture` to fetch drivers, results, and laps concurrently from OpenF1.

**Caffeine caching**: All expensive service methods are `@Cacheable`. Cache is configured in `application.properties` with a 1-hour TTL and max 500 entries. Cache names include: `raceResults`, `driverStats`, `seasonRaces`, `pointsSessions`, `driverStandings`, `teamStandings`.

**WebClient buffer**: `WebClientConfig` sets a 10MB in-memory buffer to handle large OpenF1 API responses.

### Configuration

| Property | Value |
|---|---|
| Server port | 8000 |
| OpenF1 base URL | `https://api.openf1.org/v1` (via `openf1.base-url`) |
| Cache TTL | 3600s |
| Cache max size | 500 entries |

## Conventions

- Java records pour tous les DTOs et modèles immuables
- Pas de Lombok sur les records (inutile)
- Services : interface + impl seulement si mockée dans les tests, sinon classe directe
- Tous les appels HTTP externes passent par `OpenF1Client`, jamais de WebClient direct ailleurs
- Nouvelles méthodes coûteuses → toujours `@Cacheable` avec un cache name explicite
- Exceptions métier héritent de `OpenF1Exception` ou d'une exception custom du domaine
- Logs via SLF4J (pas de `System.out`)
- Endpoints REST : kebab-case dans les URLs, camelCase dans les DTOs JSON

## Gotchas

- L'API OpenF1 renvoie parfois des tableaux vides (pas des 404). Toujours check `.isEmpty()` avant d'agréger.
- Les `session_key` changent entre les saisons — ne jamais hardcoder.
- Le cache Caffeine est en mémoire → se vide au redémarrage. Pas de Redis.
- Les virtual threads ne résolvent pas le rate-limit OpenF1 : si tu lances trop de requêtes en parallèle, tu prends des 429.
- Les records Java ne sérialisent pas bien si tu ajoutes des méthodes `getXxx()` custom — Jackson les prend pour des propriétés.

## Tests

- JUnit 5 + Mockito + AssertJ
- `@WebMvcTest` pour les controllers, `@SpringBootTest` seulement pour l'intégration
- Mock `OpenF1Client` avec Mockito, ne jamais appeler la vraie API dans les tests
- Nommage : `methodName_condition_expectedResult`
- Pas de tests sur getters/setters/records triviaux