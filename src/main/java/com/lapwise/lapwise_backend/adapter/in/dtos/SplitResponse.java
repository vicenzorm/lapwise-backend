package com.lapwise.lapwise_backend.adapter.in.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Split", description = "One pace sample from Strava laps")
public record SplitResponse(
    @Schema(description = "Distance in meters", example = "50.0")
    double distanceMeters,
    @Schema(description = "Duration in seconds", example = "40")
    int durationSeconds
) {}
