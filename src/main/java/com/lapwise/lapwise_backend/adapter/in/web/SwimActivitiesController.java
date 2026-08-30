package com.lapwise.lapwise_backend.adapter.in.web;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lapwise.lapwise_backend.adapter.in.dtos.AuthErrorResponse;
import com.lapwise.lapwise_backend.adapter.in.dtos.SwimActivityPageResponse;
import com.lapwise.lapwise_backend.adapter.in.dtos.SwimActivityResponse;
import com.lapwise.lapwise_backend.adapter.in.exception.SwimActivityNotFoundException;
import com.lapwise.lapwise_backend.domain.model.SwimActivity;
import com.lapwise.lapwise_backend.domain.model.SwimActivityPage;
import com.lapwise.lapwise_backend.domain.port.in.GetSwimActivitiesUseCase;
import com.lapwise.lapwise_backend.domain.port.in.GetSwimActivityUseCase;
import com.lapwise.lapwise_backend.domain.port.in.command.GetSwimActivitiesCommand;
import com.lapwise.lapwise_backend.domain.port.in.command.GetSwimActivityCommand;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Swim Activities")
public class SwimActivitiesController {

    private final GetSwimActivitiesUseCase getSwimActivitiesUseCase;
    private final GetSwimActivityUseCase getSwimActivityUseCase;

    public SwimActivitiesController(GetSwimActivitiesUseCase getSwimActivitiesUseCase, GetSwimActivityUseCase getSwimActivityUseCase) {
        this.getSwimActivitiesUseCase = getSwimActivitiesUseCase;
        this.getSwimActivityUseCase = getSwimActivityUseCase;
    }

    @GetMapping("/swim-activities")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(
        summary = "List synced swims",
        description = """
            Reads Postgres for the JWT user. Newest first. \
            Pass the previous page's nextCursor as cursor. Omit cursor on the first request. \
            limit defaults to 20, max 50.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "One page of swims",
            content = @Content(schema = @Schema(implementation = SwimActivityPageResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "invalid_cursor",
            content = @Content(schema = @Schema(implementation = AuthErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid session JWT",
            content = @Content(schema = @Schema(implementation = AuthErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "user_not_found",
            content = @Content(schema = @Schema(implementation = AuthErrorResponse.class))
        )
    })
    public SwimActivityPageResponse listSwimActivities(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(required = false) UUID cursor,
        @RequestParam(defaultValue = "20") int limit
    ) {
        int capped = Math.min(Math.max(limit, 1), 50);
        UUID userId = UUID.fromString(jwt.getSubject());
        SwimActivityPage page = getSwimActivitiesUseCase.getSwimActivities(
            new GetSwimActivitiesCommand(userId, cursor, capped)
        );
        return new SwimActivityPageResponse(
            page.items().stream().map(SwimActivitiesController::toResponse).toList(),
            page.nextCursor()
        );
    }


    @GetMapping("/swim-activities/{id}")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(
        summary = "Get one synced swim",
        description = "Lapwise id from the list. 404 if missing or owned by another user. Splits and insight are not loaded yet."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Swim found",
            content = @Content(schema = @Schema(implementation = SwimActivityResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid session JWT",
            content = @Content(schema = @Schema(implementation = AuthErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "swim_activity_not_found",
            content = @Content(schema = @Schema(implementation = AuthErrorResponse.class))
        )
    })
    public SwimActivityResponse getSwimActivityById(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        SwimActivity activity = getSwimActivityUseCase
            .getSwimActivity(new GetSwimActivityCommand(id, userId))
            .orElseThrow(SwimActivityNotFoundException::new);
        return toResponse(activity);
    }

    private static SwimActivityResponse toResponse(SwimActivity activity) {
        return new SwimActivityResponse(
            activity.id(),
            activity.startedAt(),
            activity.durationSeconds(),
            activity.distanceMeters()
        );
    }

}
