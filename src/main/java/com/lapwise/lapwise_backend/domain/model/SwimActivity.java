package com.lapwise.lapwise_backend.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SwimActivity(
    UUID id,
    UUID userId,
    Long stravaActivityId,
    Instant startedAt,
    int durationSeconds,
    double distanceMeters,
    Double poolLengthMeters, 
    List<Split> splits
) {
    public static SwimActivity createNew(
        UUID userId,
        Long stravaActivityId,
        Instant startedAt,
        int durationSeconds,
        double distanceMeters,
        Double poolLengthMeters,
        List<Split> splits
    ) {
        return new SwimActivity(
            null,
            userId,
            stravaActivityId,
            startedAt,
            durationSeconds,
            distanceMeters, 
            poolLengthMeters, 
            splits
        );
    }
}
