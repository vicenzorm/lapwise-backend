package com.lapwise.lapwise_backend.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ActivityInsight(
    UUID id,
    UUID activityId,
    String body,
    Instant createdAt
) {
    public static ActivityInsight createNew(UUID activityId, String body) {
        return new ActivityInsight(null, activityId, body, Instant.now());
    }
}
