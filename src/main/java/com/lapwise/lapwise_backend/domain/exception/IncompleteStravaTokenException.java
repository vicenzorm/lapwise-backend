package com.lapwise.lapwise_backend.domain.exception;

public class IncompleteStravaTokenException extends RuntimeException {
    public IncompleteStravaTokenException() {
        super("Strava token response was incomplete");
    }
}
