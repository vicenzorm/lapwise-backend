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
import com.lapwise.lapwise_backend.adapter.in.exception.InvalidOAuthStateException;
import com.lapwise.lapwise_backend.adapter.in.exception.MissingAuthorizationCodeException;
import com.lapwise.lapwise_backend.adapter.in.exception.StravaConsentDeniedException;
import com.lapwise.lapwise_backend.domain.model.User;
import com.lapwise.lapwise_backend.domain.port.in.CompleteStravaAuthUseCase;
import com.lapwise.lapwise_backend.domain.port.in.command.CompleteStravaAuthCommand;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Auth")
public class StravaAuthController {

    private static final String STATE_COOKIE = "lapwise_oauth_state";

    private final CompleteStravaAuthUseCase completeStravaAuthUseCase;
    private final String clientId;
    private final String redirectUri;

    public StravaAuthController(
        CompleteStravaAuthUseCase completeStravaAuthUseCase,
        @Value("${lapwise.strava.client-id}") String clientId,
        @Value("${lapwise.strava.redirect-uri}") String redirectUri
    ) {
        this.completeStravaAuthUseCase = completeStravaAuthUseCase;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
    }

    @GetMapping("/auth/strava/authorize")
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
    public ResponseEntity<AuthCompletedResponse> callback(
        @RequestParam(value = "error", required = false) String error,
        @RequestParam(value = "code", required = false) String code,
        @RequestParam(value = "state", required = false) String state,
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
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, clearState.toString())
            .body(new AuthCompletedResponse(user.id()));
    }
}
