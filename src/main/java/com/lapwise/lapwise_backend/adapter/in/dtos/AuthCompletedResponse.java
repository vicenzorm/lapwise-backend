package com.lapwise.lapwise_backend.adapter.in.dtos;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "AuthCompletedResponse",
    description = """
        Successful Strava callback. Persist `sessionToken` on the device (Keychain). \
        Use `userId` as the Lapwise user id. Do not persist Strava tokens from this API — \
        they are not in this payload.
        """
)
public record AuthCompletedResponse(
    @Schema(
        description = "Lapwise user id (JWT `sub` after you send sessionToken as Bearer)",
        example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    UUID userId,
    @Schema(
        description = "HS256 JWT for this API. 30-day expiry. Header: Authorization: Bearer <this value>",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIzZmE4NWY2NC01NzE3LTQ1NjItYjNmYy0yYzk2M2Y2NmFmYTYifQ.example",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String sessionToken
) {}
