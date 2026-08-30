package com.lapwise.lapwise_backend.domain.exception;

public class InsightUnavailableException extends RuntimeException {
    public InsightUnavailableException() {
        super("Insight service unavailable.");
    }
}
