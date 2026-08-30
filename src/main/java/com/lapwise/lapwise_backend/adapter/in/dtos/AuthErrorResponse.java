package com.lapwise.lapwise_backend.adapter.in.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "ErrorResponse",
    description = """
        Stable `error` for the iOS client; `message` is safe to show. \
        Never contains raw Strava JSON.
        """
)
public record AuthErrorResponse(
    @Schema(
        description = "Machine-readable code",
        example = "invalid_state",
        requiredMode = Schema.RequiredMode.REQUIRED,
        allowableValues = {
            "strava_denied",
            "missing_code",
            "invalid_state",
            "invalid_cursor",
            "incomplete_strava_token",
            "strava_unavailable",
            "user_not_found",
            "swim_activity_not_found",
            "strava_rate_limited",
            "unauthorized"
        }
    )
    String error,
    @Schema(
        description = "Human-readable explanation. No upstream Strava body.",
        example = "OAuth state cookie did not match the state query parameter",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String message
) {}
