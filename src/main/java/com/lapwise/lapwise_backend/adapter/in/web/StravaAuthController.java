package com.lapwise.lapwise_backend.adapter.in.web;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.lapwise.lapwise_backend.adapter.in.dtos.AuthCompletedResponse;
import com.lapwise.lapwise_backend.adapter.in.dtos.AuthErrorResponse;
import com.lapwise.lapwise_backend.adapter.in.exception.InvalidOAuthStateException;
import com.lapwise.lapwise_backend.adapter.in.exception.MissingAuthorizationCodeException;
import com.lapwise.lapwise_backend.adapter.in.exception.StravaConsentDeniedException;
import com.lapwise.lapwise_backend.adapter.in.helpers.LapwiseSessionIssuer;
import com.lapwise.lapwise_backend.domain.model.User;
import com.lapwise.lapwise_backend.domain.port.in.CompleteStravaAuthUseCase;
import com.lapwise.lapwise_backend.domain.port.in.command.CompleteStravaAuthCommand;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Auth")
@SecurityRequirements
public class StravaAuthController {

    private static final String STATE_COOKIE = "lapwise_oauth_state";

    private final CompleteStravaAuthUseCase completeStravaAuthUseCase;
    private final String clientId;
    private final String redirectUri;
    private final LapwiseSessionIssuer sessionIssuer;

    public StravaAuthController(
        CompleteStravaAuthUseCase completeStravaAuthUseCase,
        @Value("${lapwise.strava.client-id}") String clientId,
        @Value("${lapwise.strava.redirect-uri}") String redirectUri,
        LapwiseSessionIssuer sessionIssuer
    ) {
        this.completeStravaAuthUseCase = completeStravaAuthUseCase;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.sessionIssuer = sessionIssuer;
    }

    @GetMapping("/auth/strava/authorize")
    @Operation(
        summary = "Start Strava OAuth (browser only)",
        description = """
            Sets an HttpOnly cookie `lapwise_oauth_state` (10 minutes, path `/auth/strava`) and \
            redirects (302) to Strava's authorize URL with `client_id`, `redirect_uri`, \
            `scope=read,activity:read_all`, and `state` matching the cookie.

            Open this URL in a browser. Do not use Swagger Try it out (redirect + cookie will not complete).

            After consent, Strava sends the user to `GET /auth/strava/callback`.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "302",
            description = "Redirect to https://www.strava.com/oauth/authorize",
            headers = {
                @Header(
                    name = "Location",
                    description = "Strava consent URL",
                    schema = @Schema(type = "string", format = "uri")
                ),
                @Header(
                    name = "Set-Cookie",
                    description = "lapwise_oauth_state=<uuid>; HttpOnly; Path=/auth/strava; Max-Age=600; SameSite=Lax"
                )
            }
        )
    })
    public ResponseEntity<Void> authorize() {
        String state = UUID.randomUUID().toString();
        ResponseCookie stateCookie = ResponseCookie.from(STATE_COOKIE, state)
            .httpOnly(true)
            .path("/auth/strava")
            .maxAge(Duration.ofMinutes(10))
            .sameSite("Lax")
            .build();

        var stravaAuthorize = UriComponentsBuilder
            .fromUriString("https://www.strava.com/oauth/authorize")
            .queryParam("client_id", clientId)
            .queryParam("response_type", "code")
            .queryParam("redirect_uri", redirectUri)
            .queryParam("approval_prompt", "auto")
            .queryParam("scope", "read,activity:read_all")
            .queryParam("state", state)
            .encode()
            .build()
            .toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.SET_COOKIE, stateCookie.toString())
            .location(stravaAuthorize)
            .build();
    }

    @GetMapping("/auth/strava/callback")
    @Operation(
        summary = "Strava OAuth callback",
        description = """
            Strava redirects here with `code` and `state`. `state` must match the `lapwise_oauth_state` cookie. \
            The server exchanges `code` for Strava tokens, upserts User by `stravaAthleteId`, and returns a Lapwise JWT.

            Inbound adapter only: HTTP + cookie + JWT issue. Domain `CompleteStravaAuthUseCase` does the token exchange and persist.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "User upserted; session JWT issued. State cookie cleared.",
            content = @Content(schema = @Schema(implementation = AuthCompletedResponse.class)),
            headers = @Header(
                name = "Set-Cookie",
                description = "Clears lapwise_oauth_state (Max-Age=0)"
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "`strava_denied` (user refused consent), `missing_code`, or `invalid_state`",
            content = @Content(schema = @Schema(implementation = AuthErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "422",
            description = "`incomplete_strava_token` — Strava token JSON missing athlete id or expiry",
            content = @Content(schema = @Schema(implementation = AuthErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "502",
            description = "`strava_unavailable` — Strava HTTP failed; message does not include the upstream body",
            content = @Content(schema = @Schema(implementation = AuthErrorResponse.class))
        )
    })
    public ResponseEntity<AuthCompletedResponse> callback(
        @Parameter(description = "Strava error code when the athlete denied consent (e.g. access_denied)")
        @RequestParam(value = "error", required = false) String error,
        @Parameter(description = "Authorization code to exchange for Strava tokens. Required on success.")
        @RequestParam(value = "code", required = false) String code,
        @Parameter(description = "Must match the lapwise_oauth_state cookie from authorize")
        @RequestParam(value = "state", required = false) String state,
        @Parameter(hidden = true)
        @CookieValue(value = STATE_COOKIE, required = false) String cookieState
    ) {
        ResponseCookie clearState = ResponseCookie.from(STATE_COOKIE, "")
            .httpOnly(true)
            .path("/auth/strava")
            .maxAge(0)
            .sameSite("Lax")
            .build();

        if (error != null) {
            throw new StravaConsentDeniedException(error);
        }
        if (code == null || code.isBlank()) {
            throw new MissingAuthorizationCodeException();
        }
        if (state == null || cookieState == null || !state.equals(cookieState)) {
            throw new InvalidOAuthStateException();
        }

        User user = completeStravaAuthUseCase.complete(new CompleteStravaAuthCommand(code));
        String session = sessionIssuer.issue(user.id());
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, clearState.toString())
            .body(new AuthCompletedResponse(user.id(), session));
    }
}
