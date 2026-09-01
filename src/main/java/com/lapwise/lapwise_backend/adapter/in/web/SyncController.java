package com.lapwise.lapwise_backend.adapter.in.web;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lapwise.lapwise_backend.adapter.in.dtos.AuthErrorResponse;
import com.lapwise.lapwise_backend.adapter.in.dtos.SyncResponse;
import com.lapwise.lapwise_backend.domain.model.SyncResult;
import com.lapwise.lapwise_backend.domain.port.in.SyncActivitiesUseCase;
import com.lapwise.lapwise_backend.domain.port.in.command.SyncActivitiesCommand;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Sync")
public class SyncController {

    private static final Logger log = LoggerFactory.getLogger(SyncController.class);

    private final SyncActivitiesUseCase syncActivitiesUseCase;

    public SyncController(SyncActivitiesUseCase syncActivitiesUseCase) {
        this.syncActivitiesUseCase = syncActivitiesUseCase;
    }

    @PostMapping("/sync")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(
        summary = "Pull new swims from Strava",
        description = """
            JWT `sub` is the Lapwise user id. Refreshes Strava tokens if expired, \
            lists athlete activities (newest first; import is oldest first), \
            fetches Strava activity detail for laps, persists new SwimActivity rows, \
            then updates lastSyncedAt, then backfills ActivityInsight for stored swims \
            with usable splits and no insight row yet. Empty body; do not send a user id in JSON.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Sync finished (imported / skipped counts). Missing splits still import the swim.",
            content = @Content(schema = @Schema(implementation = SyncResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid session JWT",
            content = @Content(schema = @Schema(implementation = AuthErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "user_not_found — JWT sub does not match a users row",
            content = @Content(schema = @Schema(implementation = AuthErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "strava_rate_limited (list or activity detail). lastSyncedAt is not moved. insight_rate_limited if OpenRouter returns 429 during backfill (imported swims stay).",
            content = @Content(schema = @Schema(implementation = AuthErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "502",
            description = "strava_unavailable",
            content = @Content(schema = @Schema(implementation = AuthErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "503",
            description = "insight_unavailable — OpenRouter down or timeout during backfill",
            content = @Content(schema = @Schema(implementation = AuthErrorResponse.class))
        )
    })
    public SyncResponse sync(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("POST /sync started userId={}", userId);
        SyncResult syncResult = syncActivitiesUseCase.sync(new SyncActivitiesCommand(userId));
        log.info("POST /sync finished imported={} skipped={}", syncResult.imported(), syncResult.skipped());
        return new SyncResponse(syncResult.imported(), syncResult.skipped());
    }
}
