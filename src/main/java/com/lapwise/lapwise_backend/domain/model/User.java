package com.lapwise.lapwise_backend.domain.model;

import java.time.Instant;
import java.util.UUID;

public record User(
    UUID id,
    String email,
    Long stravaAthleteId,
    String accessToken,
    String refreshToken,
    Instant tokenExpiresAt,
    Instant lastSyncedAt
) {
    public static User createNew(
        String email,
        Long stravaAthleteId,
        String accessToken,
        String refreshToken,
        Instant tokenExpiresAt
    ) {
        return new User(null, email, stravaAthleteId, accessToken, refreshToken, tokenExpiresAt, null);
    }
}
