package com.lapwise.lapwise_backend.domain.port.out;

import java.time.Instant;
import java.util.List;

import com.lapwise.lapwise_backend.domain.model.StravaActivitySummary;

public interface StravaActivityPort {
    List<StravaActivitySummary> listAthleteActivities(String accessToken, Instant after);
}
