package com.lapwise.lapwise_backend.domain.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.lapwise.lapwise_backend.domain.model.SwimActivity;

public interface SwimActivityRepositoryPort {
    SwimActivity save(SwimActivity swimActivity);
    boolean existsByUserIdAndStravaActivityId(UUID userId, Long stravaActivityId);
    Optional<SwimActivity> findSwimActivityByIdAndUserId(UUID id, UUID userId);
    List<SwimActivity> findPage(UUID userId, Instant cursorStartedAt, UUID cursorId, int limit);
    List<SwimActivity> findByUserIdOrderByStartedAtAsc(UUID userId);
}
