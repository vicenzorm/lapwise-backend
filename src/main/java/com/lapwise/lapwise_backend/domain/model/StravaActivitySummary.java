package com.lapwise.lapwise_backend.domain.model;

import java.time.Instant;

public record StravaActivitySummary(
    Long stravaActivityId,
    String type,
    Instant startedAt,
    int durationSeconds,
    double distanceMeters,
    Double poolLengthMeters
) { }
