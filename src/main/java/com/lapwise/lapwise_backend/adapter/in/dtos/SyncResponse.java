package com.lapwise.lapwise_backend.adapter.in.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "How many swims this POST /sync imported vs skipped as duplicates")
public record SyncResponse(
    @Schema(description = "New SwimActivity rows", example = "3")
    int imported,
    @Schema(description = "Already stored for this user + Strava activity id", example = "1")
    int skipped
) {}
