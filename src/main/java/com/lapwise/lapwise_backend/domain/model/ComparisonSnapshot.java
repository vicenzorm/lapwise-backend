package com.lapwise.lapwise_backend.domain.model;

import java.util.List;

public record ComparisonSnapshot(
    double distanceMeters,
    int durationSeconds,
    double avgPacePer100m,
    double fadePercent,
    List<ComparableSwim> comparables
) {}
