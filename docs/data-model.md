# Data model

Tables live in `adapter.out.persistence`. Domain types in `domain.model` stay free of JPA.

What follows is the table list and why it is this short. The column layout is at the bottom.

## What has to survive a restart

Lapwise is not a cache in front of Strava. After `/sync`, the iOS list must still work weeks later with Strava rate-limited or down. Anything you would show on list/detail, plus the tokens needed to sync again, belongs in Postgres.

That is three durable facts:

1. Who connected Strava (and how we call Strava again).
2. Which swims we already pulled.
3. The insight we generated, when we did.



## Tables we keep



### `users`

One row per person who completed Strava OAuth.

We do not copy the Strava athlete profile. We are not building a social graph. We need a stable Lapwise id, the Strava athlete id so a second login updates the same row, and the Strava access/refresh pair so `/sync` does not ask the human to consent again.

`last_synced_at` sits here, not on a sync-run table. The PRD is one on-demand pull per user, not a history of jobs. A timestamp is the cursor. Null means never synced.

Strava tokens stay on this row. A `strava_credentials` table would only pay off with a second provider. There is one: Strava.

Email is nullable. Strava often does not give it. Unique when present so we do not invent a second User for the same inbox later.

### `swim_activities`

One row per swim we imported.

List and detail read this table, not Strava. Unique `(user_id, strava_activity_id)` so a second `/sync` does not insert duplicates. Duration, distance, start time, optional pool length, and the raw splits blob are enough to render a row and to feed the insight prompt.

We do not store runs, rides, or other Strava types. The filter happens at sync time; the table only holds swims.

### `activity_insights`

One row per SwimActivity that got an insight, unique on `activity_id`.

This could have been a nullable column on `swim_activities`. It is a separate table because the lifecycle is different: the activity is saved in phase 3 with no insight; the text appears in phase 5; some activities never get one (no splits → 422, not a half-filled swim row). `created_at` is about the generation, not the swim.

The join is 1:1. That is fine. The domain already named Insight as its own thing.

## Tables we do not have

`splits`**.** Splits are prompt raw material and detail payload, not something we query (`slowest lap`, `splits where pace > X`). Normalizing them would be a table we never filter on. They stay `jsonb` on `swim_activities`, null when Strava omitted them.

`sync_runs` **/** `jobs`**.** Sync is a blocking `POST` for 1–3s. No queue, no SSE in the MVP. A run history would be infrastructure looking for a consumer.

`sessions`**.** The iOS app will hold a Lapwise session token, not Strava's. Where that token lives (opaque row vs signed JWT) is the user story for issuing that token. It is not required to persist Users and swims. Add a table then if the token is opaque and must be revoked.

`oauth_states`**.** CSRF `state` for the authorize redirect can be a signed value in the URL, not a row. If we later need server-side one-time states, that is a small table in the auth adapter, not part of the swim model.

`athletes` **separate from** `users`**.** One human, one Strava athlete, one Lapwise User. Splitting them is a second identity we would have to keep in sync for no current feature.

## Layout

Hibernate (`spring.jpa.hibernate.ddl-auto=update`) creates these tables from `@Entity` classes in `adapter.out.persistence`. You do not write `CREATE TABLE`. Flyway can wait until the shape stops moving.

How that looks in Java: [jpa-mapping.md](jpa-mapping.md).

### `users`


| Column              | Type                     | Notes                               |
| ------------------- | ------------------------ | ----------------------------------- |
| `id`                | `uuid` PK                | Lapwise id, not Strava's            |
| `email`             | `text` unique, null      | Unique when present                 |
| `strava_athlete_id` | `bigint` unique not null | One User per Strava athlete         |
| `access_token`      | `text` not null          | Strava                              |
| `refresh_token`     | `text` not null          | Strava                              |
| `token_expires_at`  | `timestamptz` not null   | Strava access expiry                |
| `last_synced_at`    | `timestamptz` null       | `/sync` cursor. Null = never synced |




### `swim_activities`


| Column               | Type                         | Notes                     |
| -------------------- | ---------------------------- | ------------------------- |
| `id`                 | `uuid` PK                    |                           |
| `user_id`            | `uuid` not null FK → `users` |                           |
| `strava_activity_id` | `bigint` not null            |                           |
| `started_at`         | `timestamptz` not null       | PRD "data"                |
| `duration_seconds`   | `integer` not null           |                           |
| `distance_meters`    | `double precision` not null  |                           |
| `pool_length_meters` | `double precision` null      | Only when Strava sends it |
| `raw_splits_json`    | `jsonb` null                 | Split list as returned    |


Unique `(user_id, strava_activity_id)`.

### `activity_insights`


| Column        | Type                                          | Notes                                    |
| ------------- | --------------------------------------------- | ---------------------------------------- |
| `id`          | `uuid` PK                                     |                                          |
| `activity_id` | `uuid` not null unique FK → `swim_activities` | At most one Insight per swim             |
| `body`        | `text` not null                               | Generated prose                          |
| `created_at`  | `timestamptz` not null                        | When we generated it, not when they swam |


