package com.lapwise.lapwise_backend.adapter.in.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import com.lapwise.lapwise_backend.adapter.in.dtos.AuthErrorResponse;
import com.lapwise.lapwise_backend.domain.exception.IncompleteStravaTokenException;

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
}
