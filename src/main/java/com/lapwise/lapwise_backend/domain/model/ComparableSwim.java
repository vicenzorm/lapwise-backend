package com.lapwise.lapwise_backend.domain.model;

import java.time.Instant;

public record ComparableSwim(
    Instant startedAt,
    double distanceMeters,
    double avgPacePer100m,
    double fadePercent
) {}
