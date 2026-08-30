package com.lapwise.lapwise_backend.domain.usecase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.lapwise.lapwise_backend.domain.exception.InsightUnavailableException;
import com.lapwise.lapwise_backend.domain.model.ActivityInsight;
import com.lapwise.lapwise_backend.domain.model.ComparisonSnapshot;
import com.lapwise.lapwise_backend.domain.model.Split;
import com.lapwise.lapwise_backend.domain.model.StravaActivitySummary;
import com.lapwise.lapwise_backend.domain.model.StravaTokenSet;
import com.lapwise.lapwise_backend.domain.model.SwimActivity;
import com.lapwise.lapwise_backend.domain.model.User;
import com.lapwise.lapwise_backend.domain.port.in.command.SyncActivitiesCommand;
import com.lapwise.lapwise_backend.domain.port.out.ActivityInsightRepositoryPort;
import com.lapwise.lapwise_backend.domain.port.out.InsightPort;
import com.lapwise.lapwise_backend.domain.port.out.StravaActivityPort;
import com.lapwise.lapwise_backend.domain.port.out.StravaAuthPort;
import com.lapwise.lapwise_backend.domain.port.out.SwimActivityRepositoryPort;
import com.lapwise.lapwise_backend.domain.port.out.UserRepositoryPort;

class SwimActivityServiceInsightTest {

    @Test
    void firstSwimHasNoComparables_secondSeesTheFirst() {
        UUID userId = UUID.randomUUID();
        FakeSwimActivityRepository swims = new FakeSwimActivityRepository();
        FakeActivityInsightRepository insights = new FakeActivityInsightRepository();
        RecordingInsightPort insightPort = new RecordingInsightPort("ok");
        Instant older = Instant.parse("2024-01-01T12:00:00Z");
        Instant newer = Instant.parse("2024-01-08T12:00:00Z");

        SwimActivityService service = service(
            user(userId),
            new FakeStravaActivityPort(
                List.of(
                    summary(20L, newer, 300),
                    summary(10L, older, 300)
                ),
                Map.of(
                    10L, usableSplits(),
                    20L, usableSplits()
                )
            ),
            swims,
            insights,
            insightPort
        );

        service.sync(new SyncActivitiesCommand(userId));

        assertEquals(2, insightPort.snapshots.size());
        assertTrue(insightPort.snapshots.get(0).comparables().isEmpty());
        assertEquals(1, insightPort.snapshots.get(1).comparables().size());
        assertEquals(older, insightPort.snapshots.get(1).comparables().get(0).startedAt());
        assertEquals(2, insights.byActivityId.size());
    }

    @Test
    void fewerThanThreeSplits_skipsInsightRow() {
        UUID userId = UUID.randomUUID();
        FakeActivityInsightRepository insights = new FakeActivityInsightRepository();
        RecordingInsightPort insightPort = new RecordingInsightPort("ok");

        SwimActivityService service = service(
            user(userId),
            new FakeStravaActivityPort(
                List.of(summary(10L, Instant.parse("2024-01-01T12:00:00Z"), 200)),
                Map.of(10L, List.of(new Split(100, 80), new Split(100, 90)))
            ),
            new FakeSwimActivityRepository(),
            insights,
            insightPort
        );

        service.sync(new SyncActivitiesCommand(userId));

        assertTrue(insightPort.snapshots.isEmpty());
        assertTrue(insights.byActivityId.isEmpty());
    }

    @Test
    void failingInsightPort_leavesSwimForNextSync() {
        UUID userId = UUID.randomUUID();
        User storedUser = user(userId);
        FakeUserRepository users = new FakeUserRepository(storedUser);
        FakeSwimActivityRepository swims = new FakeSwimActivityRepository();
        FakeActivityInsightRepository insights = new FakeActivityInsightRepository();
        FakeStravaActivityPort strava = new FakeStravaActivityPort(
            List.of(summary(10L, Instant.parse("2024-01-01T12:00:00Z"), 300)),
            Map.of(10L, usableSplits())
        );

        SwimActivityService failing = new SwimActivityService(
            users,
            new UnusedStravaAuthPort(),
            strava,
            swims,
            insights,
            (snapshot, splits) -> {
                throw new InsightUnavailableException();
            }
        );

        assertThrows(InsightUnavailableException.class, () -> failing.sync(new SyncActivitiesCommand(userId)));
        assertEquals(1, swims.byId.size());
        assertTrue(insights.byActivityId.isEmpty());

        strava.summaries = List.of();
        RecordingInsightPort working = new RecordingInsightPort("backfilled");
        SwimActivityService retry = new SwimActivityService(
            users,
            new UnusedStravaAuthPort(),
            strava,
            swims,
            insights,
            working
        );

        retry.sync(new SyncActivitiesCommand(userId));

        assertEquals(1, insights.byActivityId.size());
        assertEquals("backfilled", insights.byActivityId.values().iterator().next().body());
        assertEquals(1, working.snapshots.size());
        assertTrue(working.snapshots.get(0).comparables().isEmpty());
    }

    private static SwimActivityService service(
        User user,
        StravaActivityPort strava,
        SwimActivityRepositoryPort swims,
        ActivityInsightRepositoryPort insights,
        InsightPort insightPort
    ) {
        return new SwimActivityService(
            new FakeUserRepository(user),
            new UnusedStravaAuthPort(),
            strava,
            swims,
            insights,
            insightPort
        );
    }

    private static User user(UUID userId) {
        return new User(
            userId,
            "vicenzo@example.com",
            1L,
            "access",
            "refresh",
            Instant.now().plusSeconds(3600),
            null
        );
    }

    private static StravaActivitySummary summary(long stravaId, Instant startedAt, double distanceMeters) {
        return new StravaActivitySummary(stravaId, "Swim", startedAt, 270, distanceMeters, null);
    }

    private static List<Split> usableSplits() {
        return List.of(
            new Split(100, 80),
            new Split(100, 90),
            new Split(100, 100)
        );
    }

    private static final class RecordingInsightPort implements InsightPort {
        private final String body;
        private final List<ComparisonSnapshot> snapshots = new ArrayList<>();

        private RecordingInsightPort(String body) {
            this.body = body;
        }

        @Override
        public String generate(ComparisonSnapshot snapshot, List<Split> thisSwimSplits) {
            snapshots.add(snapshot);
            return body;
        }
    }

    private static final class UnusedStravaAuthPort implements StravaAuthPort {
        @Override
        public StravaTokenSet exchangeAuthorizationCode(String code) {
            throw new UnsupportedOperationException();
        }

        @Override
        public StravaTokenSet refreshAccessToken(String refreshToken) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeUserRepository implements UserRepositoryPort {
        private User user;

        private FakeUserRepository(User user) {
            this.user = user;
        }

        @Override
        public User save(User user) {
            this.user = user;
            return user;
        }

        @Override
        public Optional<User> findByStravaAthleteId(Long id) {
            return Optional.empty();
        }

        @Override
        public Optional<User> findById(UUID id) {
            if (user != null && user.id().equals(id)) {
                return Optional.of(user);
            }
            return Optional.empty();
        }

        @Override
        public void deleteUserById(UUID id) {
        }
    }

    private static final class FakeStravaActivityPort implements StravaActivityPort {
        private List<StravaActivitySummary> summaries;
        private final Map<Long, List<Split>> splitsByStravaId;

        private FakeStravaActivityPort(
            List<StravaActivitySummary> summaries,
            Map<Long, List<Split>> splitsByStravaId
        ) {
            this.summaries = summaries;
            this.splitsByStravaId = splitsByStravaId;
        }

        @Override
        public List<StravaActivitySummary> listAthleteActivities(String accessToken, Instant after) {
            return summaries;
        }

        @Override
        public List<Split> getActivitySplits(String accessToken, Long stravaActivityId) {
            List<Split> splits = splitsByStravaId.get(stravaActivityId);
            return splits == null ? List.of() : splits;
        }
    }

    private static final class FakeSwimActivityRepository implements SwimActivityRepositoryPort {
        private final Map<UUID, SwimActivity> byId = new HashMap<>();

        @Override
        public SwimActivity save(SwimActivity swimActivity) {
            UUID id = swimActivity.id() == null ? UUID.randomUUID() : swimActivity.id();
            SwimActivity stored = new SwimActivity(
                id,
                swimActivity.userId(),
                swimActivity.stravaActivityId(),
                swimActivity.startedAt(),
                swimActivity.durationSeconds(),
                swimActivity.distanceMeters(),
                swimActivity.poolLengthMeters(),
                swimActivity.splits()
            );
            byId.put(id, stored);
            return stored;
        }

        @Override
        public boolean existsByUserIdAndStravaActivityId(UUID userId, Long stravaActivityId) {
            for (SwimActivity swim : byId.values()) {
                if (swim.userId().equals(userId) && swim.stravaActivityId().equals(stravaActivityId)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Optional<SwimActivity> findSwimActivityByIdAndUserId(UUID id, UUID userId) {
            SwimActivity swim = byId.get(id);
            if (swim == null || !swim.userId().equals(userId)) {
                return Optional.empty();
            }
            return Optional.of(swim);
        }

        @Override
        public List<SwimActivity> findPage(UUID userId, Instant cursorStartedAt, UUID cursorId, int limit) {
            return List.of();
        }

        @Override
        public List<SwimActivity> findByUserIdOrderByStartedAtAsc(UUID userId) {
            List<SwimActivity> rows = new ArrayList<>();
            for (SwimActivity swim : byId.values()) {
                if (swim.userId().equals(userId)) {
                    rows.add(swim);
                }
            }
            rows.sort(Comparator
                .comparing(SwimActivity::startedAt)
                .thenComparing(SwimActivity::id));
            return rows;
        }
    }

    private static final class FakeActivityInsightRepository implements ActivityInsightRepositoryPort {
        private final Map<UUID, ActivityInsight> byActivityId = new HashMap<>();

        @Override
        public ActivityInsight save(ActivityInsight activityInsight) {
            UUID id = activityInsight.id() == null ? UUID.randomUUID() : activityInsight.id();
            ActivityInsight stored = new ActivityInsight(
                id,
                activityInsight.activityId(),
                activityInsight.body(),
                activityInsight.createdAt()
            );
            byActivityId.put(stored.activityId(), stored);
            return stored;
        }

        @Override
        public Optional<ActivityInsight> findActivityInsightBySwimActivityId(UUID swimActivityId) {
            return Optional.ofNullable(byActivityId.get(swimActivityId));
        }
    }
}
