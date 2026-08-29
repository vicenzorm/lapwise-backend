# Lapwise backend

Spring Boot REST API for Lapwise: Strava OAuth2, on-demand swim sync, optional AI insight per new activity.

This repo is the API only. The iOS client is `lapwise-frontend/` (sibling git repo). Product decisions live in [`../docs/prd-lapwise.md`](../docs/prd-lapwise.md) when this workspace is opened as a whole; if that path is missing, the contract below is the source of truth.

## Authorship

Write each entity and endpoint by hand. The insight feature may call an AI API; AI does not author this service. OpenAPI generators, Spring Initializr dumps, and wholesale file trees are out.

When pairing, change the file the human named.

## Architecture — hexagonal, strict

Domain in the center. Adapters at the edge. Dependencies point inward only.

| Layer | Owns | May depend on |
|---|---|---|
| Domain | entities, value objects, ports (interfaces), use cases | the domain package only |
| Inbound adapters | HTTP controllers, OAuth callback wiring | domain use cases; Spring Web |
| Outbound adapters | JPA, Strava HTTP, AI HTTP, token store | domain ports; Spring Data / RestClient |

- Domain types carry no Spring, JPA, Jackson, or servlet annotations. Persistence models live in the outbound adapter and map to domain entities.
- A use case takes ports in its constructor (or method args). It does not import `org.springframework.web`, `jakarta.persistence`, or RestClient.
- Controllers translate HTTP ↔ use case input/output. Status codes, `429` mapping, and request DTOs stay in the inbound adapter.
- Strava, the AI API, and the database are each an outbound adapter behind a port the domain named.

Place every new type under `domain`, `adapter.in`, or `adapter.out`. A type that belongs to two layers is in the wrong layer.

```
com.lapwise.lapwise_backend/
  LapwiseBackendApplication.java   composition root
  domain/model/
  domain/port/in/
  domain/port/out/
  domain/usecase/
  adapter/in/web/
  adapter/out/persistence/
  adapter/out/strava/
  adapter/out/insight/
```

Done when the new type has one home, and `domain` compiles with no framework imports.

## Data model

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

- `GET /activities?cursor=&limit=` — cursor pagination
- `GET /activities/{id}` — detail with splits and insight

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
