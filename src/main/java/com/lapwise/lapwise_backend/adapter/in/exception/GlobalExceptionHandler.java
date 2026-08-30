package com.lapwise.lapwise_backend.adapter.in.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import com.lapwise.lapwise_backend.adapter.in.dtos.AuthErrorResponse;
import com.lapwise.lapwise_backend.domain.exception.IncompleteStravaTokenException;
import com.lapwise.lapwise_backend.domain.exception.InsightRateLimitedException;
import com.lapwise.lapwise_backend.domain.exception.InsightUnavailableException;
import com.lapwise.lapwise_backend.domain.exception.InvalidSwimCursorException;
import com.lapwise.lapwise_backend.domain.exception.StravaRateLimitedException;
import com.lapwise.lapwise_backend.domain.exception.UserNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StravaConsentDeniedException.class)
    public ResponseEntity<AuthErrorResponse> stravaDenied(StravaConsentDeniedException exception) {
        return ResponseEntity.badRequest()
            .body(new AuthErrorResponse("strava_denied", exception.getMessage()));
    }

    @ExceptionHandler(MissingAuthorizationCodeException.class)
    public ResponseEntity<AuthErrorResponse> missingCode(MissingAuthorizationCodeException exception) {
        return ResponseEntity.badRequest()
            .body(new AuthErrorResponse("missing_code", exception.getMessage()));
    }

    @ExceptionHandler(InvalidOAuthStateException.class)
    public ResponseEntity<AuthErrorResponse> invalidState(InvalidOAuthStateException exception) {
        return ResponseEntity.badRequest()
            .body(new AuthErrorResponse("invalid_state", exception.getMessage()));
    }

    @ExceptionHandler(InvalidSwimCursorException.class)
    public ResponseEntity<AuthErrorResponse> invalidCursor(InvalidSwimCursorException exception) {
        return ResponseEntity.badRequest()
            .body(new AuthErrorResponse("invalid_cursor", exception.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<AuthErrorResponse> userNotFound(UserNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new AuthErrorResponse("user_not_found", exception.getMessage()));
    }

    @ExceptionHandler(IncompleteStravaTokenException.class)
    public ResponseEntity<AuthErrorResponse> incompleteStrava(IncompleteStravaTokenException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(new AuthErrorResponse("incomplete_strava_token", exception.getMessage()));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<AuthErrorResponse> stravaHttp(RestClientException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(new AuthErrorResponse("strava_unavailable", "Strava could not complete the request"));
    }

    @ExceptionHandler(StravaRateLimitedException.class)
    public ResponseEntity<AuthErrorResponse> stravaRateLimited(StravaRateLimitedException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(new AuthErrorResponse("strava_rate_limited", exception.getMessage()));
    }

    @ExceptionHandler(InsightRateLimitedException.class)
    public ResponseEntity<AuthErrorResponse> insightRateLimited(InsightRateLimitedException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(new AuthErrorResponse("insight_rate_limited", exception.getMessage()));
    }

    @ExceptionHandler(InsightUnavailableException.class)
    public ResponseEntity<AuthErrorResponse> insightUnavailable(InsightUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new AuthErrorResponse("insight_unavailable", exception.getMessage()));
    }

    @ExceptionHandler(SwimActivityNotFoundException.class)
    public ResponseEntity<AuthErrorResponse> swimActivityNotFound(SwimActivityNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new AuthErrorResponse("swim_activity_not_found", exception.getMessage()));
    }
}
