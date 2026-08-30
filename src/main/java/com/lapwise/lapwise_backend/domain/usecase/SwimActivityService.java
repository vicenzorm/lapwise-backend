package com.lapwise.lapwise_backend.domain.usecase;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.lapwise.lapwise_backend.domain.exception.InvalidSwimCursorException;
import com.lapwise.lapwise_backend.domain.exception.UserNotFoundException;
import com.lapwise.lapwise_backend.domain.model.StravaActivitySummary;
import com.lapwise.lapwise_backend.domain.model.StravaTokenSet;
import com.lapwise.lapwise_backend.domain.model.SwimActivity;
import com.lapwise.lapwise_backend.domain.model.SwimActivityPage;
import com.lapwise.lapwise_backend.domain.model.SyncResult;
import com.lapwise.lapwise_backend.domain.model.User;
import com.lapwise.lapwise_backend.domain.port.in.GetSwimActivitiesUseCase;
import com.lapwise.lapwise_backend.domain.port.in.GetSwimActivityUseCase;
import com.lapwise.lapwise_backend.domain.port.in.SyncActivitiesUseCase;
import com.lapwise.lapwise_backend.domain.port.in.command.GetSwimActivitiesCommand;
import com.lapwise.lapwise_backend.domain.port.in.command.GetSwimActivityCommand;
import com.lapwise.lapwise_backend.domain.port.in.command.SyncActivitiesCommand;
import com.lapwise.lapwise_backend.domain.port.out.StravaActivityPort;
import com.lapwise.lapwise_backend.domain.port.out.StravaAuthPort;
import com.lapwise.lapwise_backend.domain.port.out.SwimActivityRepositoryPort;
import com.lapwise.lapwise_backend.domain.port.out.UserRepositoryPort;

public class SwimActivityService implements SyncActivitiesUseCase, GetSwimActivitiesUseCase, GetSwimActivityUseCase { 

    private final UserRepositoryPort userRepositoryPort;
    private final StravaAuthPort stravaAuthPort;
    private final StravaActivityPort stravaActivityPort;
    private final SwimActivityRepositoryPort swimActivityRepositoryPort;

    public SwimActivityService(
        UserRepositoryPort userRepositoryPort,
        StravaAuthPort stravaAuthPort, 
        StravaActivityPort stravaActivityPort, 
        SwimActivityRepositoryPort swimActivityRepositoryPort
    ) {
        this.userRepositoryPort = userRepositoryPort;
        this.stravaAuthPort = stravaAuthPort;
        this.stravaActivityPort = stravaActivityPort;
        this.swimActivityRepositoryPort = swimActivityRepositoryPort;
    }

    @Override
    public SyncResult sync(SyncActivitiesCommand command) {   
        User user = userRepositoryPort.findById(command.userId())
            .orElseThrow(UserNotFoundException::new);
        user = withFreshStravaTokens(user);
        List<StravaActivitySummary> summaries = stravaActivityPort.listAthleteActivities(user.accessToken(), user.lastSyncedAt());
        int imported = 0;
        int skipped = 0;
        for(StravaActivitySummary summary : summaries) {
            if (swimActivityRepositoryPort.existsByUserIdAndStravaActivityId(user.id(), summary.stravaActivityId())) {
                skipped++;
            } else {
                swimActivityRepositoryPort.save(SwimActivity.createNew(
                    user.id(), 
                    summary.stravaActivityId(), 
                    summary.startedAt(), 
                    summary.durationSeconds(), 
                    summary.distanceMeters(), 
                    summary.poolLengthMeters(), 
                    null
                ));
                imported++;
            }
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
    public Optional<SwimActivity> getSwimActivity(GetSwimActivityCommand command) {
        return swimActivityRepositoryPort.findSwimActivityByIdAndUserId(command.id(), command.userId());
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
