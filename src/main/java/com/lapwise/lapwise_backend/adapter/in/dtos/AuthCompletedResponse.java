package com.lapwise.lapwise_backend.adapter.in.dtos;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lapwise user after a successful Strava OAuth callback")
public record AuthCompletedResponse(
    @Schema(description = "Lapwise user id", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    UUID userId,
    @Schema(description = "Lapwise session token", example = "TODO: create example")
    String sessionToken
) {}
