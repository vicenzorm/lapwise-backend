package com.lapwise.lapwise_backend.adapter.in.dtos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "SwimActivityDetailResponse",
    description = "One synced swim with splits. insight is null when none was generated."
)
public record SwimActivityDetailResponse(
    @Schema(description = "Lapwise swim id", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    UUID id,
    @Schema(description = "When the swim started (UTC)", example = "2024-06-01T12:00:00Z")
    Instant startedAt,
    @Schema(description = "Elapsed time in seconds", example = "1800")
    int durationSeconds,
    @Schema(description = "Distance in meters", example = "2000.0")
    double distanceMeters,
    @Schema(description = "Splits from Strava laps; empty array when Strava sent none")
    List<SplitResponse> splits,
    @Schema(description = "Generated insight, or null if none yet", nullable = true)
    ActivityInsightResponse insight
) {}
