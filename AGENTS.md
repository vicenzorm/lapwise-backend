# Lapwise backend

Spring Boot REST API for Lapwise: Strava OAuth2, on-demand swim sync, optional AI insight per new activity.

This repo is the API only. The iOS client is `lapwise-frontend/` (sibling git repo). Product decisions live in [`../docs/prd-lapwise.md`](../docs/prd-lapwise.md) when this workspace is opened as a whole; if that path is missing, the contract below is the source of truth.

## Authorship

Write each entity and endpoint by hand. The insight feature may call an AI API; AI does not author this service. OpenAPI generators, Spring Initializr dumps, and wholesale file trees are out.

When pairing, change the file the human named.

## Architecture — hexagonal, strict

The rules in this file win if they ever disagree with [`HOWITWORKS.md`](HOWITWORKS.md). That file is a long teaching note (Swift MVVM → hexagon + Spring). This file is what you follow when you add a type.

Two sides only. **Inside** is the hexagon (`domain`): model, ports, use cases. **Outside** is adapters (`adapter.in`, `adapter.out`). Dependencies point inward only.

There is no `application` package. That split (Hombergs / “Java hexagonal”) is not this repo. Ports and use cases stay in `domain`.

| Layer | Owns | May depend on |
|---|---|---|
| Domain (inside) | model, domain exceptions, inbound ports (use case interfaces + commands), outbound ports, use case implementations | the `domain` package only |
| Inbound adapters | HTTP controllers, OAuth callback, request/response DTOs, HTTP exception mapping | domain inbound ports and domain exceptions; Spring Web |
| Outbound adapters | JPA, Strava HTTP, AI HTTP | domain outbound ports; Spring Data / RestClient |

- Domain types carry no Spring, JPA, Jackson, or servlet annotations. `UserService` is a plain class. Spring constructs it from a `@Bean` on `LapwiseBackendApplication` (composition root, outside `domain`). `config` holds Spring-only wiring: OpenAPI document metadata and `SecurityFilterChain`. Neither is a port. `@Schema` on DTOs stays in `adapter.in.dtos`. Do not put Security under `adapter.out`. Outbound adapters call Strava, Postgres, or the insight API. Spring Security filters inbound HTTP.
- Persistence models live in the outbound adapter and map to domain entities. Entity, mapper, Spring Data repository, and persistence adapter share `adapter.out.persistence`. Do not add sibling `entity` / `mapper` / `repository` packages; those look like a global Spring layer, not one adapter.
- Inbound ports *are* the use case interfaces (`domain.port.in`). Implementations live in `domain.usecase`. Commands live in `domain.port.in.command`. Do not nest a `useCases` folder under `port.in`.
- A use case takes ports in its constructor. It does not import `org.springframework.web`, `jakarta.persistence`, or RestClient.
- Controllers translate HTTP ↔ use case input/output. Status codes, `429` mapping, and JSON request/response types stay in the inbound adapter: DTOs in `adapter.in.dtos` (`@Schema` for Swagger), HTTP exceptions and `@RestControllerAdvice` in `adapter.in.exception`. Controllers stay in `adapter.in.web`. No `controllers` subpackage.
- Failures the domain named (incomplete Strava token, later missing splits) are types in `domain.exception`. Do not invent exception types for features that do not exist yet.
- Strava, the AI API, and the database are each one outbound adapter behind a port the domain named.

Place every new type under `domain`, `adapter.in`, `adapter.out`, or `config`. `config` is only composition-root Spring beans (OpenAPI, Security filter chain, loading `.env` into the environment). A type that belongs to two layers is in the wrong layer.

```
com.lapwise.lapwise_backend/
  LapwiseBackendApplication.java   composition root (wires domain use cases)
  config/                          OpenAPI, SecurityFilterChain, .env loader
  domain/model/
  domain/exception/
  domain/port/in/
  domain/port/in/command/
  domain/port/out/
  domain/usecase/
  adapter/in/web/
  adapter/in/dtos/
  adapter/in/exception/
  adapter/out/persistence/
  adapter/out/strava/
  adapter/out/insight/
```

Done when the new type has one home, and `domain` compiles with no framework imports.

## Data model

Glossary: [`CONTEXT.md`](CONTEXT.md). Tables: [`docs/data-model.md`](docs/data-model.md). JPA annotations: [`docs/jpa-mapping.md`](docs/jpa-mapping.md).

| Entity | Role |
|---|---|
| `User` | id, email, stravaAthleteId, accessToken, refreshToken, tokenExpiresAt |
| `SwimActivity` | id, userId, stravaActivityId, date, duration, distance, pool (if present), rawSplitsJson |
| `ActivityInsight` | id, activityId, generated text, createdAt |

`rawSplitsJson` is the prompt raw material. Not every Strava activity has splits.

## HTTP contract

### Auth (Strava OAuth2, authorization code)

- `GET /auth/strava/authorize` — redirect to Strava consent
- `GET /auth/strava/callback` — exchange `code` for tokens; create or update `User`

Strava access/refresh is a server problem. The iOS app holds a session token for *this* API, not Strava's.

### Sync

- `POST /sync` — pull new swim activities since last sync, persist `SwimActivity`, generate `ActivityInsight` when that phase is in. Synchronous; 1–3s is acceptable. No SSE, webhook, or queue in the MVP.

### Query

- `GET /swim-activities?cursor=&limit=` — cursor pagination
- `GET /swim-activities/{id}` — detail with splits and insight

### Errors the client is built to handle

| Status | Meaning |
|---|---|
| `401` | Session token expired |
| `429` | Strava rate limit — map to a clear message, do not leak the upstream body |
| `422` | Unexpected or incomplete Strava payload (missing splits is the usual case) |
| `502` / `503` | Strava down or unstable |

## Sync flow

1. Client `POST /sync`
2. Call Strava `GET /athlete/activities`, swim-only, using the user's Strava token (refresh first if expired)
3. For each new activity: persist, then (once the AI phase is in) a short prompt from available splits on the cheapest model, no long history
4. Persist `ActivityInsight`; return the full result so the client can refresh the list

## Backend phases

1. Register the Strava app; know the rate limits
2. Authorize, callback, refresh — this is the new muscle
3. `POST /sync` with real swims, persist only
4. *(iOS — not this repo)*
5. Insight generation inside `/sync`
6. *(iOS — not this repo)*
7. Optional later: SSE on the sync button

Stop at a runnable API at the end of each backend phase.

## Out of scope (MVP)

SSE, Strava webhooks, push, manual workout APIs, distributed cache, message queues, load tests.
