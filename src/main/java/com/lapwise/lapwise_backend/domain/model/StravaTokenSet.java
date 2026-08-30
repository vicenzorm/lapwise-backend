package com.lapwise.lapwise_backend.domain.model;

import java.time.Instant;

public record StravaTokenSet(
    Long stravaAthleteId,
    String accessToken,
    String refreshToken,
    Instant tokenExpiresAt
) { }
