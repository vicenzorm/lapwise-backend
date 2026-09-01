package com.lapwise.lapwise_backend.domain.usecase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.lapwise.lapwise_backend.domain.SplitAnalytics;
import com.lapwise.lapwise_backend.domain.exception.InvalidSwimCursorException;
import com.lapwise.lapwise_backend.domain.exception.UserNotFoundException;
import com.lapwise.lapwise_backend.domain.model.ActivityInsight;
import com.lapwise.lapwise_backend.domain.model.ComparableSwim;
import com.lapwise.lapwise_backend.domain.model.ComparisonSnapshot;
import com.lapwise.lapwise_backend.domain.model.Split;
import com.lapwise.lapwise_backend.domain.model.StravaActivitySummary;
import com.lapwise.lapwise_backend.domain.model.StravaTokenSet;
import com.lapwise.lapwise_backend.domain.model.SwimActivity;
import com.lapwise.lapwise_backend.domain.model.SwimActivityDetail;
import com.lapwise.lapwise_backend.domain.model.SwimActivityPage;
import com.lapwise.lapwise_backend.domain.model.SyncResult;
import com.lapwise.lapwise_backend.domain.model.User;
import com.lapwise.lapwise_backend.domain.port.in.GetSwimActivitiesUseCase;
import com.lapwise.lapwise_backend.domain.port.in.GetSwimActivityUseCase;
import com.lapwise.lapwise_backend.domain.port.in.SyncActivitiesUseCase;
import com.lapwise.lapwise_backend.domain.port.in.command.GetSwimActivitiesCommand;
import com.lapwise.lapwise_backend.domain.port.in.command.GetSwimActivityCommand;
import com.lapwise.lapwise_backend.domain.port.in.command.SyncActivitiesCommand;
import com.lapwise.lapwise_backend.domain.port.out.ActivityInsightRepositoryPort;
import com.lapwise.lapwise_backend.domain.port.out.InsightPort;
import com.lapwise.lapwise_backend.domain.port.out.StravaActivityPort;
import com.lapwise.lapwise_backend.domain.port.out.StravaAuthPort;
import com.lapwise.lapwise_backend.domain.port.out.SwimActivityRepositoryPort;
import com.lapwise.lapwise_backend.domain.port.out.UserRepositoryPort;

public class SwimActivityService implements SyncActivitiesUseCase, GetSwimActivitiesUseCase, GetSwimActivityUseCase { 

    private final UserRepositoryPort userRepositoryPort;
    private final StravaAuthPort stravaAuthPort;
    private final StravaActivityPort stravaActivityPort;
    private final SwimActivityRepositoryPort swimActivityRepositoryPort;
    private final ActivityInsightRepositoryPort activityInsightRepositoryPort;
    private final InsightPort insightPort;

    public SwimActivityService(
        UserRepositoryPort userRepositoryPort,
        StravaAuthPort stravaAuthPort, 
        StravaActivityPort stravaActivityPort, 
        SwimActivityRepositoryPort swimActivityRepositoryPort,
        ActivityInsightRepositoryPort activityInsightRepositoryPort,
        InsightPort insightPort
    ) {
        this.userRepositoryPort = userRepositoryPort;
        this.stravaAuthPort = stravaAuthPort;
        this.stravaActivityPort = stravaActivityPort;
        this.swimActivityRepositoryPort = swimActivityRepositoryPort;
        this.activityInsightRepositoryPort = activityInsightRepositoryPort;
        this.insightPort = insightPort;
    }

    @Override
    public SyncResult sync(SyncActivitiesCommand command) {   
        User user = userRepositoryPort.findById(command.userId())
            .orElseThrow(UserNotFoundException::new);
        user = withFreshStravaTokens(user);
        
        List<StravaActivitySummary> summaries = stravaActivityPort.listAthleteActivities(user.accessToken(), user.lastSyncedAt());
        int imported = 0;
        int skipped = 0;

        for(int i = summaries.size() - 1; i >= 0; i--) {
            StravaActivitySummary summary = summaries.get(i);
            if (swimActivityRepositoryPort.existsByUserIdAndStravaActivityId(user.id(),summary.stravaActivityId())) {
                skipped++;
                continue;
            }
            List<Split> splits = stravaActivityPort.getActivitySplits(
                user.accessToken(),
                summary.stravaActivityId()
            );
            swimActivityRepositoryPort.save(SwimActivity.createNew(
                user.id(),
                summary.stravaActivityId(),
                summary.startedAt(),
                summary.durationSeconds(),
                summary.distanceMeters(),
                summary.poolLengthMeters(),
                splits.isEmpty() ? null : splits
            ));
            imported++;
        }
        User newUser = new User(
            user.id(), 
            user.email(), 
            user.stravaAthleteId(),
            user.accessToken(), 
            user.refreshToken(),
            user.tokenExpiresAt(),
            Instant.now()
        );
        userRepositoryPort.save(newUser);
        backfillInsights(user.id());
        return new SyncResult(imported, skipped);
    }   

    @Override
    public SwimActivityPage getSwimActivities(GetSwimActivitiesCommand command) {
        User user = userRepositoryPort.findById(command.userId())
            .orElseThrow(UserNotFoundException::new);
        int fetch = command.limit() + 1;
        List<SwimActivity> rows;
        if (command.cursor() == null) {
            rows = swimActivityRepositoryPort.findPage(user.id(), null, null, fetch);
        } else {
            SwimActivity cursorRow = swimActivityRepositoryPort
                .findSwimActivityByIdAndUserId(command.cursor(), user.id())
                .orElseThrow(InvalidSwimCursorException::new);
            rows = swimActivityRepositoryPort.findPage(
                user.id(),
                cursorRow.startedAt(),
                cursorRow.id(),
                fetch
            );
        }
        boolean hasMore = rows.size() > command.limit();
        List<SwimActivity> items = hasMore ? List.copyOf(rows.subList(0, command.limit())) : rows;
        UUID nextCursor = hasMore ? items.get(items.size() - 1).id() : null;
        return new SwimActivityPage(items, nextCursor);
    }

    @Override
    public Optional<SwimActivityDetail> getSwimActivity(GetSwimActivityCommand command) {
        return swimActivityRepositoryPort
            .findSwimActivityByIdAndUserId(command.id(), command.userId())
            .map(activity -> new SwimActivityDetail(
                activity,
                activityInsightRepositoryPort.findActivityInsightBySwimActivityId(activity.id()).orElse(null)
            ));
    }

    private void backfillInsights(UUID userId) {
        List<SwimActivity> swims = swimActivityRepositoryPort.findByUserIdOrderByStartedAtAsc(userId);
        List<SwimActivity> earlier = new ArrayList<>();
        for (SwimActivity swim : swims) {
            if (SplitAnalytics.isUsable(swim.splits())
                && activityInsightRepositoryPort.findActivityInsightBySwimActivityId(swim.id()).isEmpty()) {
                List<SwimActivity> comparableSwims = SplitAnalytics.selectComparables(swim, earlier);
                List<ComparableSwim> comparables = new ArrayList<>();
                for (SwimActivity candidate : comparableSwims) {
                    if (!SplitAnalytics.isUsable(candidate.splits())) {
                        continue;
                    }
                    comparables.add(SplitAnalytics.toComparable(candidate, candidate.splits()));
                }
                List<Split> paced = SplitAnalytics.pacedSplits(swim.splits());
                ComparisonSnapshot snapshot = SplitAnalytics.snapshot(swim, paced, comparables);
                String body = insightPort.generate(snapshot, paced);
                if (body != null && !body.isBlank()) {
                    activityInsightRepositoryPort.save(ActivityInsight.createNew(swim.id(), body));
                }
            }
            earlier.add(swim);
        }
    }

    private User withFreshStravaTokens(User user) {
        if(user.tokenExpiresAt().isAfter(Instant.now())) {
            return user;
        } else {
            StravaTokenSet tokens = stravaAuthPort.refreshAccessToken(user.refreshToken());
            User updatedUser = new User(
                user.id(),
                user.email(),
                user.stravaAthleteId(),
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.tokenExpiresAt(),
                user.lastSyncedAt()
            );
            return userRepositoryPort.save(updatedUser);
        }
    }
}
