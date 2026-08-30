package com.lapwise.lapwise_backend.domain.exception;

public class StravaRateLimitedException extends RuntimeException {
    public StravaRateLimitedException() {
        super("Strava rate limit reached. Try again later.");
    }
}