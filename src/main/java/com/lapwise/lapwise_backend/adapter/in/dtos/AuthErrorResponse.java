package com.lapwise.lapwise_backend.adapter.in.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error returned by this API")
public record AuthErrorResponse(
    @Schema(description = "Stable error code for the client", example = "invalid_state")
    String error,
    @Schema(description = "Human-readable message. Does not include upstream Strava bodies")
    String message
) {}
