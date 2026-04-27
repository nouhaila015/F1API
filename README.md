# F1 API

A Spring Boot 3 / Java 21 REST API that aggregates Formula 1 data from [OpenF1](https://openf1.org) and computes live standings and driver statistics.

## Features

- Race and sprint session results
- Driver and constructor championship standings
- Per-driver season stats (points, wins, podiums, DNFs, best finish)
- Official F1 scoring — GP top 10, Sprint top 8, fastest-lap bonus
- Caffeine in-memory cache with 1-hour TTL
- Auto-syncs data from OpenF1 on first request per season

## Requirements

- Java 21+
- Maven (wrapper included)
- PostgreSQL (running locally or via Docker)

## Getting started

```bash
# Start the server (port 8000)
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Build a JAR
./mvnw clean package
```

Configure the database connection in `src/main/resources/application.properties` before starting.

## API

Interactive docs (Swagger UI) are available at `http://localhost:8000/swagger-ui.html` once the server is running.

### Races

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/races/{year}/season` | All Grand Prix sessions for a season |
| GET | `/api/races/{sessionKey}/results` | Results for a specific session |
| GET | `/api/races/drivers/{driverNumber}/stats?year=` | Driver season stats |

### Standings

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/standings/{year}/drivers` | Driver championship standings |
| GET | `/api/standings/{year}/teams` | Constructor championship standings |

### Example

```bash
# 2024 driver standings
curl http://localhost:8000/api/standings/2024/drivers

# Results for a specific race session
curl http://localhost:8000/api/races/9149/results

# Verstappen's 2024 stats
curl "http://localhost:8000/api/races/drivers/1/stats?year=2024"
```

## Architecture

```
Controller → Service → Repository (PostgreSQL)
                 ↑
         DataSyncService → OpenF1Client → https://api.openf1.org/v1
```

- **`controller/`** — REST endpoints (`RaceController`, `StandingsController`)
- **`service/`** — Business logic (`SessionService`, `RaceService`, `ScoringService`)
- **`sync/DataSyncService`** — Fetches and persists OpenF1 data on demand; rate-limited to avoid 429s
- **`client/OpenF1Client`** — WebClient wrapper for all external HTTP calls
- **`model/`** — Raw OpenF1 JSON records (snake_case via Jackson)
- **`dto/`** — API response objects (aggregated/computed fields)
- **`utils/F1ScoringUtils`** — Point tables and fastest-lap bonus logic

## Caching

All expensive reads are `@Cacheable` (Caffeine, in-memory, 1-hour TTL). The cache is **not** persisted — it resets on restart. Cache names: `raceResults`, `driverStats`, `seasonRaces`, `pointsSessions`, `driverStandings`, `teamStandings`.
