package com.lapwise.lapwise_backend.adapter.in.web;

import java.util.UUID;

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
            lists athlete activities, persists new SwimActivity rows, updates lastSyncedAt. \
            No insight. Empty body; do not send a user id in JSON.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Sync finished",
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
            responseCode = "502",
            description = "strava_unavailable",
            content = @Content(schema = @Schema(implementation = AuthErrorResponse.class))
        )
    })
    public SyncResponse sync(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        SyncResult syncResult = syncActivitiesUseCase.sync(new SyncActivitiesCommand(userId));
        return new SyncResponse(syncResult.imported(), syncResult.skipped());
    }
}
