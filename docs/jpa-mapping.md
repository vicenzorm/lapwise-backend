# Spring Data JPA — this schema as annotations

Java calls them **annotations**, not decorators. Same idea: metadata on a class or field that Hibernate reads to build SQL.

You already have the ORM (`spring-boot-starter-data-jpa`). This file is the translation of [data-model.md](data-model.md) into those annotations. Put the classes in `adapter.out.persistence`. Leave `domain.model` as plain Java with no `jakarta.persistence` imports.

Hibernate then creates the tables (`ddl-auto=update`). You still type the entity classes by hand.

Package for examples: `com.lapwise.lapwise_backend.adapter.out.persistence`.

## Two layers of “Spring”

| Piece | Package | Role |
|---|---|---|
| JPA | `jakarta.persistence.*` | `@Entity`, `@Table`, `@Column`, relations — the mapping |
| Spring Data | `org.springframework.data.jpa.repository.*` | `JpaRepository<UserEntity, UUID>` — save/find without writing SQL |

Spring Boot auto-configures a `DataSource` from `application.properties` and scans for `@Entity` under the application package.

## Annotation cheat sheet

| Annotation | Goes on | Meaning |
|---|---|---|
| `@Entity` | class | This type is a table row |
| `@Table(name = "…")` | class | Table name (default would be the class name) |
| `@Id` | field | Primary key |
| `@GeneratedValue(strategy = GenerationType.UUID)` | id field | Hibernate assigns a UUID on insert |
| `@Column` | field | Column name, nullability, uniqueness |
| `@ManyToOne` | field | Many rows point at one parent (`swim_activities.user_id`) |
| `@OneToOne` | field | Exactly one insight per activity |
| `@JoinColumn` | relation field | The FK column on *this* table |
| `@JdbcTypeCode(SqlTypes.JSON)` | field | Store Java value as JSON / `jsonb` |

`FetchType.LAZY` on relations: do not load the User when you load a swim unless you ask. Default `@ManyToOne` is EAGER — set LAZY on purpose.

`Instant` maps cleanly to `timestamptz`.

## `users` → `UserEntity`

```java
@Entity
@Table(name = "users")
public class UserEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(unique = true)
	private String email; // null allowed: omit nullable = false

	@Column(name = "strava_athlete_id", nullable = false, unique = true)
	private Long stravaAthleteId;

	@Column(name = "access_token", nullable = false)
	private String accessToken;

	@Column(name = "refresh_token", nullable = false)
	private String refreshToken;

	@Column(name = "token_expires_at", nullable = false)
	private Instant tokenExpiresAt;

	@Column(name = "last_synced_at")
	private Instant lastSyncedAt;
}
```

`nullable = false` is the opposite of a SQL `NULL` column. If you omit `@Column`, Hibernate still creates a column; the annotation is how you pin the name and nullability to the doc.

## `swim_activities` → `SwimActivityEntity`

Composite unique key lives on `@Table`, not on one field:

```java
@Entity
@Table(
	name = "swim_activities",
	uniqueConstraints = @UniqueConstraint(
		columnNames = { "user_id", "strava_activity_id" }
	)
)
public class SwimActivityEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@Column(name = "strava_activity_id", nullable = false)
	private Long stravaActivityId;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "duration_seconds", nullable = false)
	private Integer durationSeconds;

	@Column(name = "distance_meters", nullable = false)
	private Double distanceMeters;

	@Column(name = "pool_length_meters")
	private Double poolLengthMeters;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "raw_splits_json", columnDefinition = "jsonb")
	private String rawSplitsJson;
}
```

`@ManyToOne` + `@JoinColumn` is the FK. You do not declare a separate `UUID userId` field unless you want a read-only copy (`insertable = false, updatable = false`) — one or the other, not both as writable.

`columnDefinition = "jsonb"` keeps Postgres from creating a generic JSON type. `SqlTypes.JSON` tells Hibernate to serialize the `String` as JSON.

## `activity_insights` → `ActivityInsightEntity`

```java
@Entity
@Table(name = "activity_insights")
public class ActivityInsightEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "activity_id", nullable = false, unique = true)
	private SwimActivityEntity activity;

	@Column(nullable = false)
	private String body;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;
}
```

`unique = true` on the join column is “at most one insight per swim.”

## Repositories (Spring Data)

```java
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
	Optional<UserEntity> findByStravaAthleteId(Long stravaAthleteId);
}
```

Spring implements `findByStravaAthleteId` from the method name. No `@Repository` required on the interface; the `JpaRepository` extension is enough.

These interfaces are still the persistence adapter. A domain port like `UserRepositoryPort` is implemented by a class that calls `UserJpaRepository` and maps `UserEntity` ↔ domain `User`.

## What not to annotate

- Domain `User` / `SwimActivity` / `Insight` — no `@Entity`
- Controllers — they never see `UserEntity`
- `package-info.java` — ignore

When you write the real classes, start with `UserEntity` and `UserJpaRepository`; Hibernate will create `users` on the next boot against the `lapwise` database.
