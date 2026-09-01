# Activity insight via OpenRouter

Phase 5 of Lapwise. GitHub epic [#17](https://github.com/vicenzorm/lapwise-backend/issues/17). Approved 2026-08-30.

## Problem

Strava does not narrate pace fade inside a swim, and a prompt that only sees one activity cannot honestly compare it to earlier swims. The insight on detail must do both: read this session’s splits, and mention similar recent swims, without turning the model into a training planner.

## Decision

**Narrator port.** Domain Java computes the numbers. OpenRouter writes one short paragraph. One `ActivityInsight` per `SwimActivity`, shown on `GET /swim-activities/{id}`. No separate coach entity, no dumping prior swims’ raw splits into the prompt.

## What an Insight is

A short observation of this swim versus its splits and versus similar recent swims, plus at most one cautious note tied to those numbers. No weekly plan, no injury talk, no invented history. At most one row per swim. Absent when Strava sent no usable splits.

## Placement

| Piece | Home |
|---|---|
| `Split`, `ActivityInsight`, `ComparisonSnapshot` | `domain.model` |
| Fade % and comparable-peer selection | `domain` helper, no Spring |
| `InsightPort.generate(snapshot, thisSwimSplits) → String` | `domain.port.out` |
| `ActivityInsightRepositoryPort` | `domain.port.out` |
| OpenRouter HTTP | `adapter.out.insight` |
| JPA | `adapter.out.persistence.activityinsight` (entity already exists) |
| Strava activity detail (splits) | `adapter.out.strava` |

`SwimActivityService.sync` orchestrates. No new HTTP endpoint. No `application` package. Domain compiles with no RestClient, Jackson annotations, or OpenRouter types.

`rawSplitsJson` stays `jsonb` on `swim_activities` for persistence and detail. Fade math uses domain `Split` values. The Strava adapter maps laps into those values; the domain does not parse JSON.

## Sync loop

`POST /sync` stays one blocking request. It may take much longer than 1–3s when several new swims each need a Strava detail call and an OpenRouter completion. That is accepted for this phase. No queue, no SSE.

Two steps in the same POST:

1. **Import.** Refresh Strava tokens if needed. List athlete activities. For each new swim, oldest first (reverse Strava’s newest-first list): `GET /api/v3/activities/{id}` for laps, persist `SwimActivity` with `rawSplitsJson` when Strava sent splits. Then set `lastSyncedAt`.
2. **Backfill insights.** For this user’s swims that have usable splits and no `ActivityInsight` yet, oldest first: load comparable peers, build `ComparisonSnapshot`, call `InsightPort`, persist `ActivityInsight`.

If OpenRouter fails mid-loop, imported swims stay. The next `POST /sync` resumes step 2 without re-pulling those activities from Strava.

If Strava returns 429 on the list or a detail fetch, abort and do not move `lastSyncedAt` (same as today’s list-call behaviour).

### Usable splits

At least three splits, each with distance and a duration (moving time if present, else elapsed). Fewer than three: persist the swim, skip insight.

### Comparable peers

Same user, already in Postgres, distance within ±20% of this swim, up to 5, most recent first, excluding this swim. Swims imported earlier in this same POST count. Zero peers is allowed (first swim): the snapshot says so; the prompt must not invent history.

### Fade

Split the ordered splits into three contiguous groups as evenly as possible. Fade % = `(lastGroupAvgPace − firstGroupAvgPace) / firstGroupAvgPace`, where pace is seconds per 100 m. A warmup 50 m does not dominate lap 1 vs lap N.

### Snapshot (what the model receives)

- This swim: distance, duration, avg pace, fade %, and the raw split list (distance + duration each).
- Each peer: started at, distance, avg pace, fade %. Not that peer’s splits.

## Prompt

One chat completion per insight. Model id is `OPENROUTER_MODEL` (not hardcoded in domain). Default in `.env.example` is `google/gemini-2.5-flash-lite`: cheap, thinking off unless you enable it. The adapter always sends `reasoning.effort=none` and `exclude=true`, and maps only `choices[0].message.content`. Low temperature, small max tokens.

System: short observation of this swim vs the snapshot numbers; at most one cautious note; no invented peers; no injury or training-plan.

User: the snapshot plus this swim’s splits.

API key from `.env` (`OPENROUTER_API_KEY`). Never put tokens or keys in the prompt.

## HTTP

**List** unchanged: no splits, no insight.

**`GET /swim-activities/{id}`** (not `/activities/{id}`): current swim fields, splits, and `insight` as `{ body, createdAt }` or `null`. No generate-on-read.

**`POST /sync`:** same status codes as today, plus OpenRouter mapping below. Empty body. SyncResponse can stay imported/skipped counts.

| Upstream | Lapwise | `error` code |
|---|---|---|
| OpenRouter 429 | 429 | `insight_rate_limited` |
| OpenRouter down / timeout | 503 | `insight_unavailable` |
| Empty or garbage completion | no HTTP error | skip that swim; step 2 retries next sync |
| Missing usable splits | no HTTP error | swim stored, no insight row |
| Strava 429 | 429 | `strava_rate_limited` (unchanged) |

Missing splits do **not** 422 the sync. `422` remains for other unusable Strava payloads (e.g. incomplete token).

## Tests

No live OpenRouter in CI.

- Domain: fade % and ±20% / max 5 peer selection on fixed split lists; fewer than 3 splits is not usable.
- Use case with fakes: first swim has no peers; second sees the first; no usable splits → no insight row; failing `InsightPort` leaves the swim for the next sync’s backfill.
- Adapter: JSON mapping; 429 → `insight_rate_limited`; timeout/5xx → `insight_unavailable`.

## Out of scope

RAG, embeddings, tool-calling, a user-level coach table, dumping prior swims’ raw splits, insight regeneration, SSE, queues, generate-on-GET.
